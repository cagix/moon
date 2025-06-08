(ns clojure.gdx.java
  (:import (com.badlogic.gdx Gdx)))

(defn context []
  {:clojure.gdx/app      Gdx/app
   :clojure.gdx/audio    Gdx/audio
   :clojure.gdx/files    Gdx/files
   :clojure.gdx/graphics Gdx/graphics
   :clojure.gdx/input    Gdx/input})
