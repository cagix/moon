(ns clojure.gdx
  (:require [gdl.app :as app]
            [gdl.files :as files]
            [gdl.input :as input])
  (:import (com.badlogic.gdx Gdx)))

(defn app []
  (let [this Gdx/app]
    (reify app/App
      (post-runnable! [_ runnable]
        (.postRunnable this runnable)))))

(defn files []
  (let [this Gdx/files]
    (reify files/Files
      (internal [_ path]
        (.internal this path)))))
