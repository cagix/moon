(ns clojure.gdx.backends.lwjgl.start
  (:require [clojure.gdx.backends.lwjgl :as lwjgl]))

(defn application! [{:keys [config listener]}]
  (lwjgl/application! config
                      (let [[f params] listener]
                        (f params))))
