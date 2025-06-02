(ns clojure.render.stage
  (:require [clojure.ui.stage :as stage]))

(defn do! [{:keys [ctx/stage] :as ctx}]
  (stage/render! stage ctx))
