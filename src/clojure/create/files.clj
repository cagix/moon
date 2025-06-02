(ns clojure.create.files
  (:require [clojure.files :as files])
  (:import (com.badlogic.gdx Gdx)))

(defn do! [ctx]
  (assoc ctx :ctx/files (let [this Gdx/files]
                          (reify files/Files
                            (internal [_ path]
                              (.internal this path))))))
