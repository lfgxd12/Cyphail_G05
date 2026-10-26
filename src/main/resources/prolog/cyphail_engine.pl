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
