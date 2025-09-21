(ns cdq.application.create
  (:require [cdq.application :as application]
            [cdq.ctx :as ctx]
            [clojure.config :as config]
            [clojure.utils :as utils]
            [malli.core :as m]
            [malli.utils]
            [qrecord.core :as q]))

(def ^:private schema
  (m/schema [:map {:closed true}
             [:ctx/audio :some]
             [:ctx/editor :some]
             [:ctx/db :some]
             [:ctx/graphics :some]

             ; FIXME
             [:ctx/input :some]
             [:ctx/controls :some]

             ; FIXME
             [:ctx/stage :some]
             [:ctx/vis-ui :some]
             [:ctx/ui-actors :some]

             [:ctx/world :some]

             ; FIXME
             [:ctx/mouseover-actor :any]
             [:ctx/ui-mouse-position :some]
             [:ctx/world-mouse-position :some]
             [:ctx/interaction-state :some]]))

(q/defrecord Context [ctx/graphics]
  ctx/Validation
  (validate [this]
    (malli.utils/validate-humanize schema this)
    this))

(def ^:private create-pipeline (config/edn-resource "create.edn"))

(defn do!
  [{:keys [gdl/audio
           gdl/files
           gdl/graphics
           gdl/input]}]
  (reset! application/state (utils/pipeline
                             (merge (map->Context {})
                                    {:ctx/audio audio
                                     :ctx/files files
                                     :ctx/graphics graphics
                                     :ctx/input input})
                             create-pipeline)))
