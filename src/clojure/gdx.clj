(ns clojure.gdx
  (:import (com.badlogic.gdx Gdx)))

(defn context []
  {:clojure/app           Gdx/app
   :clojure/audio         Gdx/audio
   :clojure/files         Gdx/files
   :clojure/graphics      Gdx/graphics
   :clojure.graphics/gl   Gdx/gl
   :clojure.graphics/gl20 Gdx/gl20
   :clojure.graphics/gl30 Gdx/gl30
   :clojure.graphics/gl31 Gdx/gl31
   :clojure.graphics/gl32 Gdx/gl32
   :clojure/input         Gdx/input
   :clojure/net           Gdx/net})
