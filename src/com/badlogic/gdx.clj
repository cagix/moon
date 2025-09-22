(ns com.badlogic.gdx
  (:import (com.badlogic.gdx Gdx)))

(defn state []
  {:ctx/app      Gdx/app
   :ctx/audio    Gdx/audio
   :ctx/files    Gdx/files
   :ctx/graphics Gdx/graphics
   :ctx/input    Gdx/input})
