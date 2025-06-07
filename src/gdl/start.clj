(ns gdl.start
  (:require [clojure.gdx.app-listener :as app-listener]
            [clojure.gdx.backends.lwjgl :as lwjgl]))

(defn start! [{:keys [config listener]}]
  (lwjgl/application! config
                      (app-listener/create-adapter listener)))
