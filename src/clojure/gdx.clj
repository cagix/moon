(ns clojure.gdx
  (:import (com.badlogic.gdx Gdx)))

(defn get-context []
  {::app      Gdx/app
   ::files    Gdx/files
   ::graphics Gdx/graphics
   ::input    Gdx/input})
