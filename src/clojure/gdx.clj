(ns clojure.gdx
  (:require [clojure.files])
  (:import (com.badlogic.gdx Gdx)))

(defn files [_context]
  (let [files Gdx/files]
    (reify clojure.files/Files
      (internal [_ path]
        (.internal files path)))))
