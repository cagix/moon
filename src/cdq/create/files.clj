(ns cdq.create.files
  (:require [gdl.files])
  (:import (com.badlogic.gdx Gdx
                             Files)))

(defn- make-files [^Files files]
  (reify gdl.files/Files
    (internal [_ path]
      (.internal files path))))

(defn do! [ctx]
  (assoc ctx :ctx/files (make-files Gdx/files)))
