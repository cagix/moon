(ns cdq.application.create.record
  (:require [cdq.ctx :as ctx]
            [cdq.malli :as m]
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
             [:ctx/info :some]

             ; FIXME
             [:ctx/mouseover-actor :any]
             [:ctx/ui-mouse-position :some]
             [:ctx/world-mouse-position :some]
             [:ctx/interaction-state :some]]))

; <- this is a performance optimization qrecord
(q/defrecord Context [ctx/audio
                      ctx/editor
                      ctx/db
                      ctx/graphics
                      ctx/input
                      ctx/controls]
  ctx/Validation
  (validate [this]
    (m/validate-humanize schema this)
    this))

(defn do! [ctx]
  (merge (map->Context {})
         ctx))
