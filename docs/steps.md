1. step:

'cdq' project should not see any 'clojure.gdx, badlogic, kotcrab, space.earlygrey'
    => move this into separate library & remove from the project
        & make tested & API

    -> these dependencies:
                 [clojure.gdx.backends.lwjgl                  ~libgdx-version]
                 [com.badlogicgames.gdx/gdx                   ~libgdx-version]
                 [com.badlogicgames.gdx/gdx-freetype          ~libgdx-version]
                 [com.badlogicgames.gdx/gdx-freetype-platform ~libgdx-version :classifier "natives-desktop"]
                 [space.earlygrey/shapedrawer "2.5.0"]
                 [com.kotcrab.vis/vis-ui "1.5.2"]

                 1.1: can I make it state-free?


                 1.2 docs:
                    It gives you a context:
          {:ctx/input  -> protocol gdl.input
           :ctx/graphics -> protocol gdl.graphics
           :ctx/assets  -> protocol gdl.assets
           :ctx/ui-viewport -> protocol gdl.viewport
           :ctx/stage  -> ?
