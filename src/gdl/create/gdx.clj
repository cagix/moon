(ns gdl.create.gdx
  (:import (com.badlogic.gdx Gdx)))

; I can not set the Gdx statics with my own Lwjgl3Application
; only - where are they used?
; I can create alternated mathods which pass the param
(defn do! [ctx]
  (merge ctx {:ctx/app      Gdx/app
              :ctx/audio    Gdx/audio
              :ctx/files    Gdx/files
              :ctx/graphics Gdx/graphics
              :ctx/input    Gdx/input}))
