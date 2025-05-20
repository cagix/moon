(ns cdq.application
  (:require [cdq.ctx :as ctx]
            [gdl.application :as application]))

(def create-fns '[[:ctx/config [cdq.config/create "config.edn"]]
                  cdq.application.create.requires/do!
                  [:ctx/db [cdq.db/create "properties.edn" "schema.edn"]]
                  [:ctx/assets [cdq.assets/create {:folder "resources/"
                                                   :asset-type-extensions {:sound   #{"wav"}
                                                                           :texture #{"png" "bmp"}}}]]
                  [:ctx/batch [cdq.application.create.batch/do!]]
                  [:ctx/shape-drawer-texture [cdq.application.create.shape-drawer-texture/do!]]
                  [:ctx/world-unit-scale [cdq.application.create.world-unit-scale/do!]]
                  [:ctx/shape-drawer [cdq.application.create.shape-drawer/do!]]
                  [:ctx/cursors [cdq.application.create.cursors/do!]]
                  [:ctx/default-font [cdq.application.create.default-font/do!]]
                  [:ctx/world-viewport [cdq.application.create.world-viewport/do!]]
                  [:ctx/get-tiled-map-renderer [cdq.application.create.tiled-map-renderer/do!]]
                  [:ctx/ui-viewport [cdq.application.create.ui-viewport]]
                  cdq.application.create.ui/do!
                  [:ctx/elapsed-time [cdq.elapsed-time/create]]
                  [:ctx/stage [cdq.ctx.init-stage/do!]]
                  [:ctx/level [cdq.level/create]]
                  [:ctx/grid [cdq.grid/create]]
                  [:ctx/raycaster [cdq.raycaster/create]]
                  [:ctx/content-grid [cdq.content-grid/create]]
                  [:ctx/explored-tile-corners [cdq.explored-tile-corners/create]]
                  [:ctx/id-counter [cdq.id-counter/create]]
                  [:ctx/entity-ids [cdq.entity-ids/create]]
                  [:ctx/potential-field-cache [cdq.potential-field-cache/create]]
                  cdq.ctx.spawn-enemies/do!
                  cdq.ctx.spawn-player/do!])

(def dispose-fn 'cdq.application.dispose/do!)

(def resize-fn 'cdq.application.resize/do!)

(def render-fns '[cdq.application.render.bind-active-entities/do!
                  cdq.application.render.set-camera-on-player/do!
                  cdq.application.render.clear-screen/do!
                  cdq.application.render.draw-tiled-map/do!
                  cdq.application.render.draw-on-world-viewport/do!
                  cdq.application.render.draw-ui/do!
                  cdq.application.render.update-ui/do!
                  cdq.application.render.player-state-handle-click/do!
                  cdq.application.render.update-mouseover-entity/do!
                  cdq.application.render.bind-paused/do!
                  cdq.application.render.when-not-paused/do!
                  cdq.application.render.remove-destroyed-entities/do! ; do not pause as pickup item should be destroyed
                  cdq.application.render.camera-controls/do!])

(defn create! [initial-context create-fns]
  (reduce (fn [ctx create-fn]
            (if (vector? create-fn)
              (let [[k [f & params]] create-fn]
                (assoc ctx k (apply (requiring-resolve f) params ctx)))
              (do
               ((requiring-resolve create-fn) ctx)
               ctx)))
          initial-context
          create-fns))

(def state (atom nil))

(def config
  {:title "Cyber Dungeon Quest"
   :window-width 1440
   :window-height 900
   :fps 60
   :dock-icon "moon.png"
   :create! (fn []
              (reset! state (create! {:ctx/pausing? true
                                      :ctx/zoom-speed 0.025
                                      :ctx/controls {:zoom-in :minus
                                                     :zoom-out :equals
                                                     :unpause-once :p
                                                     :unpause-continously :space}
                                      :ctx/sound-path-format "sounds/%s.wav"
                                      :ctx/unit-scale (atom 1)
                                      :ctx/mouseover-eid nil ; needed ?
                                      :ctx/effect-body-props {:width 0.5
                                                              :height 0.5
                                                              :z-order :z-order/effect}
                                      :ctx/minimum-size minimum-size
                                      :ctx/z-orders ctx/z-orders}
                                     create-fns)))
   :dispose! (fn []
               ((requiring-resolve dispose-fn) @state))
   :render! (fn []
              ; TODO render returns new ctx ...
              ; => every step a swap! ???
              (doseq [f render-fns]
                ((requiring-resolve f) (ctx/make-map))))
   :resize! (fn [_width _height]
              ((requiring-resolve resize-fn) @state))})

(defn -main []
  (application/start! config))
