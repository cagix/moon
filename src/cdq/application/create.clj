(ns cdq.application.create
  (:require [cdq.application :as application]
            [com.badlogic.gdx :as gdx]
            [clojure.config :as config]
            [clojure.utils :as utils]))

(defn do!
  []
  (reset! application/state (utils/pipeline
                             (let [{:keys [audio files graphics input]} (gdx/state)]
                               {:ctx/audio audio
                                :ctx/files files
                                :ctx/graphics graphics
                                :ctx/input input})
                             (config/edn-resource "create.edn"))))
