/** <module> Engine de Cyphail (SWI-Prolog)

    Cyphail - Graph Query Engine Prototype
    EIF400-II-2026 - Escuela de Informatica, UNA
    Grupo: G05
    Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
             Jostin Jimenez Alfaro, Angel Rojas Ruano

    Esquema del grafo (segun el SPEC del Sprint P2), para un grafo llamado G:

        user_G_node(Id).                    un nodo
        user_G_label(Id, Label).            una etiqueta de un nodo
        user_G_edge(EdgeId, Type, From, To) una relacion dirigida, con su tipo
        user_G_prop(Id, Key, Value).        una propiedad de un nodo O de una relacion
                                            (los ids de nodos y relaciones nunca se repiten)

    Los predicados con prefijo cy_ son del engine; los que empiezan con user_ describen el grafo.

    IMPORTANTE: este archivo NO puede escribir a la salida estandar (writeln, format a user_output...).
    Las dos primeras lineas que imprime "swipl mqi" son el puerto y la contrasena, y el broker de
    Java las lee de ahi. Cualquier otra salida rompe la conexion.
*/

% Todo lo que CREATE, SET, REMOVE y DELETE modifican debe ser dinamico
:- dynamic user_amigos_node/1.
:- dynamic user_amigos_label/2.
:- dynamic user_amigos_edge/4.
:- dynamic user_amigos_prop/3.

cy_db(amigos).

%%%%%%%%%% Grafo de ejemplo: amigos %%%%%%%%%%

user_amigos_node(id1).
user_amigos_node(id2).
user_amigos_node(id3).
user_amigos_node(id4).

user_amigos_label(id1, 'Person').
user_amigos_label(id2, 'Person').
user_amigos_label(id3, 'Person').
user_amigos_label(id4, 'Person').

user_amigos_prop(id1, name, 'Juan Perez').
user_amigos_prop(id1, age, 30).
user_amigos_prop(id1, gender, 'male').

user_amigos_prop(id2, name, 'Maria Salas').
user_amigos_prop(id2, age, 30).
user_amigos_prop(id2, gender, 'female').

user_amigos_prop(id3, name, 'Jorge Soto').
user_amigos_prop(id3, age, 43).
user_amigos_prop(id3, gender, 'male').

user_amigos_prop(id4, name, 'Ana Flores').
user_amigos_prop(id4, age, 21).
user_amigos_prop(id4, gender, 'female').

% Juan y Maria son amigos entre si (mismo edad, ambas direcciones)
user_amigos_edge(e1, amigo, id1, id2).
user_amigos_edge(e2, amigo, id2, id1).
user_amigos_edge(e3, amigo, id1, id4).
user_amigos_edge(e4, amigo, id3, id1).


%%%%%%%%%% Comparadores %%%%%%%%%%
% Los genera el Codegen para traducir =, <>, >, <, >=, <=. Fallan en silencio, nunca lanzan error.

cy_eq(X, Y) :-
    number(X), number(Y),
    !,
    X =:= Y.
cy_eq(X, Y) :-
    X == Y.

cy_neq(X, Y) :-
    \+ cy_eq(X, Y).

cy_gt(X, Y)  :- number(X), number(Y), X > Y.
cy_lt(X, Y)  :- number(X), number(Y), X < Y.
cy_gte(X, Y) :- number(X), number(Y), X >= Y.
cy_lte(X, Y) :- number(X), number(Y), X =< Y.


%%%%%%%%%% Formato de Rows (contrato C2) %%%%%%%%%%
% Encabezado + filas separadas por \n; cada celda entre comillas, con \, " y \n escapados.

cy_cell(Valor, Celda) :-
    cy_valor_texto(Valor, Texto),
    cy_escapar(Texto, Escapado),
    format(string(Celda), "\"~w\"", [Escapado]).

cy_valor_texto(Valor, Texto) :-
    ( number(Valor)
    -> format(string(Texto), "~w", [Valor])
    ;  atom_string(Valor, Texto)
    ).

cy_escapar(Texto, Escapado) :-
    string_codes(Texto, Codigos),
    maplist(cy_escapar_codigo, Codigos, Partes),
    atomic_list_concat(Partes, EscapadoAtomo),
    atom_string(EscapadoAtomo, Escapado).

cy_escapar_codigo(0'\\, '\\\\') :- !.
cy_escapar_codigo(0'",  '\\"')  :- !.
cy_escapar_codigo(0'\n, '\\n')  :- !.
cy_escapar_codigo(Codigo, Caracter) :-
    char_code(Char, Codigo),
    atom_string(Char, Caracter).

% Sin columnas (cabeza sin argumentos, p. ej. un CREATE sin RETURN) -> Rows vacio
cy_format_rows(Columnas, _Filas, "") :-
    Columnas == [],
    !.
cy_format_rows(Columnas, Filas, Rows) :-
    atomic_list_concat(Columnas, ' ', Encabezado),
    maplist(cy_format_fila, Filas, FilasTexto),
    atomic_list_concat([Encabezado | FilasTexto], '\n', RowsAtomo),
    atom_string(RowsAtomo, Rows).

cy_format_fila(Valores, FilaTexto) :-
    maplist(cy_cell, Valores, Celdas),
    atomic_list_concat(Celdas, ' ', FilaTexto).


%%%%%%%%%% Ciclo de vida de una consulta %%%%%%%%%%
% El Codegen encadena las tres en una sola peticion MQI.
:- dynamic cy_stored/2.

cy_create_query(Id, Clausula) :-
    gensym(cy_query_, Id),
    assertz(cy_stored(Id, Clausula)).

cy_exec_query(Id, Columnas, Rows) :-
    cy_stored(Id, (Cabeza :- Cuerpo)),
    !,
    Cabeza =.. [_Nombre | Args],
    findall(Args, Cuerpo, Filas),
    cy_format_rows(Columnas, Filas, Rows).

cy_erase_query(Id) :-
    retractall(cy_stored(Id, _)).