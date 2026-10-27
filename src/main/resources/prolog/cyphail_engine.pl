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


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% PASO 1 — Comparadores y formato de Rows
% (Persona 4 - Angel)
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

%% --- Comparadores ---
%  Los genera el Codegen para traducir "=", "<>", ">", "<", ">=", "<=".
%  Contrato: todos de 2 argumentos; FALLAN EN SILENCIO (nunca lanzan error),
%  igual que cualquier otro goal de Prolog que no se cumple.

%! cy_eq(?X, ?Y) is semidet.
%  "=" de Cyphail. Si ambos son numeros, compara por VALOR (30 =:= 30.0
%  es verdadero). Si no, por identidad de terminos (==), para que
%  'Juan Perez' == 'Juan Perez' sea verdadero pero 'Juan' \== 'Maria'.
cy_eq(X, Y) :-
    number(X), number(Y),
    !,
    X =:= Y.
cy_eq(X, Y) :-
    X == Y.

%! cy_neq(?X, ?Y) is semidet.
%  "<>" de Cyphail: lo contrario exacto de cy_eq.
cy_neq(X, Y) :-
    \+ cy_eq(X, Y).

%! cy_gt(?X, ?Y) is semidet.
%! cy_lt(?X, ?Y) is semidet.
%! cy_gte(?X, ?Y) is semidet.
%! cy_lte(?X, ?Y) is semidet.
%  ">" "<" ">=" "<=" de Cyphail. Solo tienen sentido entre numeros: si
%  cualquiera de los dos no lo es, el predicado simplemente FALLA (no
%  lanza excepcion), igual que pasaria comparando texto con numero.
cy_gt(X, Y)  :- number(X), number(Y), X > Y.
cy_lt(X, Y)  :- number(X), number(Y), X < Y.
cy_gte(X, Y) :- number(X), number(Y), X >= Y.
cy_lte(X, Y) :- number(X), number(Y), X =< Y.


%% --- Formato de Rows (contrato C2, el mismo que RowsTable en Java) ---
%
%   Rows   := Header ("\n" Fila)*       sin filas -> solo el Header
%                                        sin columnas -> string vacio
%   Header := col (" " col)*            nombres de columna tal cual, sin comillas
%   Fila   := celda (" " celda)*        una celda por columna, SIEMPRE con comillas
%   celda  := '"' contenido '"'         dentro: \ -> \\   " -> \"   salto de linea -> \n

%! cy_cell(+Valor, -Celda:string) is det.
%  Convierte un valor cualquiera (atomo, string o numero) en una celda
%  de Rows: su texto, con \, " y saltos de linea escapados, entre
%  comillas dobles. 30 -> "30" ; 'Juan Perez' -> "Juan Perez".
cy_cell(Valor, Celda) :-
    cy_valor_texto(Valor, Texto),
    cy_escapar(Texto, Escapado),
    format(string(Celda), "\"~w\"", [Escapado]).

%! cy_valor_texto(+Valor, -Texto:string) is det.
%  Texto "crudo" (sin comillas ni escapes) de un valor del grafo.
cy_valor_texto(Valor, Texto) :-
    ( number(Valor)
    -> format(string(Texto), "~w", [Valor])
    ;  atom_string(Valor, Texto)
    ).

%! cy_escapar(+Texto:string, -Escapado:string) is det.
%  Aplica, en orden, \ -> \\ , " -> \" , salto de linea -> \n.
%  El orden importa: si se escapara " antes de \, la barra que ese
%  escape agrega se volveria a escapar.
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

%! cy_format_rows(+Columnas:list(atom), +Filas:list(list), -Rows:string) is det.
%  Columnas = ['a.name','b.name'] ; Filas = lista de listas de valores
%  (una lista por fila, un valor por columna, en el mismo orden).
%  Sin columnas (cabeza sin argumentos, p. ej. un CREATE sin RETURN) ->
%  Rows es el string vacio, sin importar cuantas filas haya.
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