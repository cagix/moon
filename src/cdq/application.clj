(ns cdq.application
  (:require [cdq.malli :as m]
            [cdq.utils :as utils]
            [gdl.graphics :as graphics]
            [gdl.utils.disposable :as disp]
            [qrecord.core :as q]))

(comment
 (defmacro def-record-and-schema )
 )

(q/defrecord Context [ctx/config
                      ctx/db
                      ctx/graphics
                      ctx/input
                      ctx/stage])

(def ^:private schema
  (m/schema [:map {:closed true}
             [:ctx/config :some]

             [:ctx/db :some]

             [:ctx/audio :some]

             [:ctx/input :some]

             [:ctx/stage :some]

             [:ctx/graphics :some]
             [:ctx/world-event-handlers :some]

             ; TODO can do _first_ the renamings to ':world/'
             ; separaately from other changes

             ; our world model is most complex
             [:ctx/elapsed-time number?]
             ; info-segment <- only [ctx/elapsed-time]
             ; effect/handle <- only world
             ; entity/tick! <- FIXME cdq.entity.state.player-moving -> controls
             ; entity/create <- this is intereseting, can do only w. world?
             ; draw-active-skill [exception]
             ; cdq.render.update-time [exception]
             ; cdq.ctx.effect-handler/do! - [FIXED]
             ; cdq.ui.dev-menu [exception]
             [:ctx/delta-time {:optional true} number?]
             ; entity/tick!
             ; cdq.render.update-time
             ; cdq.tx.update-animation
             [:ctx/max-delta number?]
             ; cdq.render.update-time
             [:ctx/max-speed number?]
             ; entity/tick!
             [:ctx/minimum-size number?]
             ; spawn-entity!
             [:ctx/paused? {:optional true} :boolean]
             ; dev-menu
             [:ctx/tiled-map :some]
             ; cdq.render.draw-world-map
             [:ctx/grid :some]
             ; context-entity-add!/remove/upd
             ; cdq.ctx.grid
             ; entity/tick!
             ; cdq.render.draw-on-world-viewport.draw-cell-debug
             ; cdq.render.draw-on-world-viewport.geom-test
             ; ..
             ; cdq.render.update-mouseover-entity
             ; update-potential-fields!
             [:ctx/raycaster :some]
             ; cdq.ctx.line-of-sight
             ; effect/useful?
             ; cdq.render.draw-world-map
             [:ctx/content-grid :some]
             ; ctx add/remove/upd
             ; calculate-active-entities
             [:ctx/explored-tile-corners :some]
             [:ctx/id-counter :some]
             [:ctx/entity-ids :some]
             [:ctx/potential-field-cache :some]
             [:ctx/factions-iterations :some]
             [:ctx/z-orders :some]
             [:ctx/render-z-order :some]
             [:ctx/mouseover-eid {:optional true} :any]
             [:ctx/player-eid :some]
             [:ctx/active-entities {:optional true} :some]
             ]))

(def state (atom nil))

(defn create! [[config
                create-fns]]
  (let [config (utils/load-edn-config config)
        ctx (merge (map->Context {})
                   {:ctx/config config})
        ctx (reduce utils/render* ctx create-fns)]
    (m/validate-humanize schema ctx)
    (reset! state ctx)))

(defn dispose! []
  (let [{:keys [ctx/audio
                ctx/graphics
                ctx/tiled-map]} @state]
    (disp/dispose! audio)
    (disp/dispose! graphics)
    (disp/dispose! tiled-map)
    ; TODO vis-ui dispose
    ; TODO what else disposable?
    ; => :ctx/tiled-map definitely and also dispose when re-creting gamestate.
    ))

(defn render! [render-fns]
  (swap! state (fn [ctx]
                 (m/validate-humanize schema ctx)
                 (let [ctx (reduce utils/render* ctx render-fns)]
                   (m/validate-humanize schema ctx)
                   ctx))))

(defn resize! [width height]
  (m/validate-humanize schema @state)
  (let [{:keys [ctx/graphics]} @state]
    (graphics/resize-viewports! graphics width height)))
