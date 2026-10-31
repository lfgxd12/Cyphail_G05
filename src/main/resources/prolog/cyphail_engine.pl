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


%%%%%%%%%% Operaciones de escritura (contrato C8) %%%%%%%%%%
% Las llama el Codegen dentro del cuerpo de la consulta (una vez por cada solucion del MATCH).
% Todas reciben el nombre del grafo G. Los errores se lanzan como error(cy_error(Detalle), _)
% porque el MQI solo devuelve el detalle de los errores envueltos en error/2.

:- dynamic cy_counter/2.

% user_G_Sufijo(Args...) como termino: cy_pred(amigos, prop, 3, [Id, K, V], Goal)
cy_pred(G, Sufijo, Aridad, Args, Goal) :-
    atomic_list_concat([user_, G, '_', Sufijo], Nombre),
    length(Args, Aridad),
    Goal =.. [Nombre | Args].

% Id nuevo (id101, e101, ...) que ningun nodo ni relacion usa; los ids nunca se repiten
cy_next_id(G, Prefijo, Id) :-
    ( retract(cy_counter(Prefijo, N0)) -> true ; N0 = 100 ),
    N is N0 + 1,
    assertz(cy_counter(Prefijo, N)),
    atom_concat(Prefijo, N, Candidato),
    (   cy_id_used(G, Candidato)
    ->  cy_next_id(G, Prefijo, Id)
    ;   Id = Candidato
    ).

cy_id_used(G, Id) :-
    (   cy_pred(G, node, 1, [Id], Nodo), clause(Nodo, true)
    ;   cy_pred(G, edge, 4, [Id, _, _, _], Rel), clause(Rel, true)
    ;   cy_pred(G, prop, 3, [Id, _, _], Prop), clause(Prop, true)
    ),
    !.

% cy_create_node(+G, +Labels, +Props, -Id)   Props es una lista Clave-Valor
cy_create_node(G, Labels, Props, Id) :-
    cy_next_id(G, id, Id),
    cy_pred(G, node, 1, [Id], Nodo), assertz(Nodo),
    forall(member(L, Labels), ( cy_pred(G, label, 2, [Id, L], Et), assertz(Et) )),
    cy_assert_props(G, Id, Props).

% cy_create_edge(+G, +Type, +From, +To, +Props, -Id)
cy_create_edge(G, Type, From, To, Props, Id) :-
    cy_next_id(G, e, Id),
    cy_pred(G, edge, 4, [Id, Type, From, To], Rel), assertz(Rel),
    cy_assert_props(G, Id, Props).

cy_assert_props(G, Id, Props) :-
    forall(member(K-V, Props), ( cy_pred(G, prop, 3, [Id, K, V], P), assertz(P) )).

% cy_set_prop(+G, +Id, +K, +V)   crea la propiedad o reemplaza su valor
cy_set_prop(G, Id, K, V) :-
    cy_pred(G, prop, 3, [Id, K, _], Vieja), retractall(Vieja),
    cy_pred(G, prop, 3, [Id, K, V], Nueva), assertz(Nueva).

% cy_remove_prop(+G, +Id, +K)   si la propiedad no existe no hace nada
cy_remove_prop(G, Id, K) :-
    cy_pred(G, prop, 3, [Id, K, _], P), retractall(P).

% cy_delete(+G, +Id)   borra un nodo (que no tenga relaciones) o una relacion
cy_delete(G, Id) :-
    cy_pred(G, edge, 4, [_, _, Id, _], Sale),
    cy_pred(G, edge, 4, [_, _, _, Id], Entra),
    (   ( clause(Sale, true) ; clause(Entra, true) )
    ->  throw(error(cy_error(node_has_relationships(Id)), _))
    ;   cy_drop(G, Id)
    ).

% cy_detach_delete(+G, +Id)   borra el nodo y todas las relaciones que lo tocan
cy_detach_delete(G, Id) :-
    cy_pred(G, edge, 4, [E, _, Id, _], Sale),
    cy_pred(G, edge, 4, [E, _, _, Id], Entra),
    findall(E, ( clause(Sale, true) ; clause(Entra, true) ), Relaciones),
    forall(member(R, Relaciones), cy_drop(G, R)),
    cy_drop(G, Id).

% Quita todo lo que se sabe de un id (nodo o relacion)
cy_drop(G, Id) :-
    cy_pred(G, prop, 3, [Id, _, _], P), retractall(P),
    cy_pred(G, label, 2, [Id, _], L), retractall(L),
    cy_pred(G, node, 1, [Id], N), retractall(N),
    cy_pred(G, edge, 4, [Id, _, _, _], R), retractall(R).
