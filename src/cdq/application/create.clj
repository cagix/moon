(ns cdq.application.create
  (:require [clojure.config :as config]
            [clojure.utils :as utils]))

(def ^:private create-pipeline (config/edn-resource "create.edn"))

(defn do!
  [{:keys [clojure.gdx/audio
           clojure.gdx/files
           clojure.gdx/graphics
           clojure.gdx/input]}]
  (utils/pipeline
   {:ctx/audio audio
    :ctx/files files
    :ctx/graphics graphics
    :ctx/input input}
   create-pipeline))
