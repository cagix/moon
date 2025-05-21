(ns cdq.application
  (:require [cdq.ctx :as ctx]
            [gdl.application :as application]))

; TODO

; * startup slower -> maybe serialized require problem ?

; * world-fns dev-menu
; => need to save the steps somewhere (config?)
; * call 'create!' ...

; * editor save/delete
; => pass state to each step and swap there?? idk

; * render make declare as side-effect-y -> no need return nil always ?
; assoc/update/side-effects,etc.

; * click sometimes not working! ( stage & input click handler outside of stage is catched ?)
;  -> comment out stage & check

(def initial-context
  {:ctx/pausing? true
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
   :ctx/minimum-size ctx/minimum-size
   :ctx/z-orders ctx/z-orders})

; TODO do not complect configuration with order -> no params ?

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
                  [:ctx/ui-viewport [cdq.application.create.ui-viewport/do!]]
                  cdq.application.create.ui/do!


                  [:ctx/elapsed-time [cdq.elapsed-time/create]]
                  [:ctx/stage [cdq.ctx.init-stage/do!]]

                  [:ctx/level [cdq.level/create cdq.level.vampire/create]]
                  [:ctx/tiled-map [cdq.level/tiled-map]]
                  [:ctx/start-position [cdq.level/start-position]]

                  [:ctx/grid [cdq.grid/create]]
                  [:ctx/raycaster [cdq.raycaster/create]]
                  [:ctx/content-grid [cdq.content-grid/create]]
                  [:ctx/explored-tile-corners [cdq.explored-tile-corners/create]]
                  [:ctx/id-counter [cdq.id-counter/create]]
                  [:ctx/entity-ids [cdq.entity-ids/create]]
                  [:ctx/potential-field-cache [cdq.potential-field-cache/create]]
                  cdq.ctx.spawn-enemies/do!
                  [:ctx/player-eid [cdq.ctx.spawn-player/do!]]])

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

(defn render-ctx! [initial-context render-fns]
  (reduce (fn [ctx render-fn]
            (if-let [result ((requiring-resolve render-fn) ctx)]
              result
              ctx))
          initial-context
          render-fns))

(defn create-ctx! [initial-context create-fns]
  (reduce (fn [ctx create-fn]
            (if (vector? create-fn)
              (let [[k [f & params]] create-fn]
                (assoc ctx k (apply (requiring-resolve f) ctx params)))
              (do
               ((requiring-resolve create-fn) ctx)
               ctx)))
          initial-context
          create-fns))

(def state (atom nil))

(comment
  (clojure.pprint/pprint (sort (keys @state)))
 )

(defn create! []
  (reset! state (create-ctx! initial-context create-fns)))

(defn dispose! []
  ((requiring-resolve dispose-fn) @state))

(defn render! []
  (swap! state render-ctx! render-fns))

(defn resize! [_width _height]
  ((requiring-resolve resize-fn) @state))

(def config
  {:title "Cyber Dungeon Quest"
   :window-width 1440
   :window-height 900
   :fps 60
   :dock-icon "moon.png"
   :create! create!
   :dispose! dispose!
   :render! render!
   :resize! resize!})

(defn -main []
  (application/start! config))
