(ns cdq.application.create
  (:require [cdq.application :as application]
            [clojure.config :as config]
            [clojure.utils :as utils]))

(defn do!
  [{:keys [gdl/audio
           gdl/files
           gdl/graphics
           gdl/input]}]
  (reset! application/state (utils/pipeline
                             {:ctx/audio audio
                              :ctx/files files
                              :ctx/graphics graphics
                              :ctx/input input}
                             (config/edn-resource "create.edn"))))
