(ns gdl.create.gdx
  (:import (com.badlogic.gdx Gdx)))

(defn do! [ctx]
  (merge ctx {:ctx/app      Gdx/app
              :ctx/audio    Gdx/audio
              :ctx/files    Gdx/files
              :ctx/graphics Gdx/graphics
              :ctx/input    Gdx/input}))
