(ns gdl.create.gdx
  (:import (com.badlogic.gdx Gdx)))

(defn do! [_ctx _params]
  {:app      Gdx/app
   :audio    Gdx/audio
   :files    Gdx/files
   :graphics Gdx/graphics
   :input    Gdx/input})
