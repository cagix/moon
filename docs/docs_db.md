
Db _WORKS_

the main thing is that it works then you can make it simpler

so you _need_ tests then you can change stuff

=> gdl tests then I have an amazing library ... !

stateless hould be !?

everything should depend on only abstract/concept

e.g. app depends on a 'game-world' and just the things:
- create
- dispose
- render

=> that's it... maybe the world-viewport is part of it

so => tests ! they improve design of your code

e.g. animation-test, draw-tiled-map test (only batch, viewport!?, no UI viewport?! => basic building blocks are viewports, etc. mostly gdx stuff )

=> 2d library like slick2d with tests,examples and basic abstraction over libgdx to build 2d desktop games! thats the whole point!

like gdx-tests...!

=> then I have standalone components!!!


GDL!!!

1. animation (or a test runner?) or separate tests just app w. animation?

1. start an empty window!

=> classic games implement! space invaders, snake,etc. !!!!
simple roguelike??
why not ! small games!! then document how you write it ! a book ! a website ! an engine !
step by step with example repos !!

=> Now I look into gdl.app code what to test => an API with nice treeview animated & ICONS ICONS ICONS ICONS !!!!!



checklist in the tests - the tests as game - confirm this works - -> automated test system
-> do for all effects, etc.
=> DB!

minimal tests/examples!

w. taskbar - icon ?

domain specific language

(app/def batch (g/sprite-batch))
(even for viewports?!)

=> on app close will be disposed
=> will be loaded on app start

=> check slick2d tests ? libgdx tests? just translate them to clojure all gdx tests???
or play-clj tests???? improve just play-clj??? code is already there?!?!!?

=> first of all I don't even have an API
