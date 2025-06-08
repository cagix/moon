(ns clojure.gdx.java
  (:require [clojure.gdx.app])
  (:import (com.badlogic.gdx Application
                             Gdx)))

(defn- reify-app [^Application this]
  (reify clojure.gdx.app/Application
    (post-runnable! [_ runnable]
      (.postRunnable this runnable))))

(defn context []
  {:clojure.gdx/app      (reify-app Gdx/app)
   :clojure.gdx/audio    Gdx/audio
   :clojure.gdx/files    Gdx/files
   :clojure.gdx/graphics Gdx/graphics
   :clojure.gdx/input    Gdx/input})
