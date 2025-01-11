(ns clojure.gdx
  (:import (com.badlogic.gdx Gdx)))

(defn context []
  {:clojure/app      Gdx/app
   :clojure/audio    Gdx/audio
   :clojure/files    Gdx/files
   :clojure/gl       Gdx/gl
   :clojure/gl20     Gdx/gl20
   :clojure/gl30     Gdx/gl30
   :clojure/gl31     Gdx/gl31
   :clojure/gl32     Gdx/gl32
   :clojure/graphics Gdx/graphics
   :clojure/input    Gdx/input
   :clojure/net      Gdx/net})
