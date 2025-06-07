(ns gdl.start
  (:require [clojure.gdx.backends.lwjgl :as lwjgl]))

(defn start! [{:keys [config listener]}]
  (lwjgl/application! config
                      (let [[f params] listener]
                        (f params))))
