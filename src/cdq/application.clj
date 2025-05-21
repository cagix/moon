(ns cdq.application
  (:require [cdq.ctx :as ctx]
            [gdl.application :as application]))

; TODO

; * startup slower -> maybe serialized require problem ?
; => beceause so many namespaces ?!

; * world-fns dev-menu
; => need to save the steps somewhere (config?)
; * call 'create!' ...

; * editor save/delete
; => pass state to each step and swap there?? idk

; * render make declare as side-effect-y -> no need return nil always ?
; assoc/update/side-effects,etc.

; * click sometimes not working! ( stage & input click handler outside of stage is catched ?)
;  -> comment out stage & check
; => issue disappeared after restart (mac os problem?)

; TODO do not complect configuration with order -> no params ?

(def create-app-state
  '[[:ctx/config [cdq.create.config/create "config.edn"]]
    cdq.create.requires/create
    [:ctx/db [cdq.db/create "properties.edn" "schema.edn"]]
    [:ctx/assets [cdq.create.assets/create {:folder "resources/"
                                            :asset-type-extensions {:sound   #{"wav"}
                                                                    :texture #{"png" "bmp"}}}]]
    [:ctx/batch [cdq.create.batch/do!]]
    [:ctx/shape-drawer-texture [cdq.create.shape-drawer-texture/do!]]
    [:ctx/world-unit-scale [cdq.create.world-unit-scale/do!]]
    [:ctx/shape-drawer [cdq.create.shape-drawer/do!]]
    [:ctx/cursors [cdq.create.cursors/do!]]
    [:ctx/default-font [cdq.create.default-font/do!]]
    [:ctx/world-viewport [cdq.create.world-viewport/do!]]
    [:ctx/get-tiled-map-renderer [cdq.create.tiled-map-renderer/do!]]
    [:ctx/ui-viewport [cdq.create.ui-viewport/do!]]
    cdq.create.ui/do!])

(def create-game-state
  '[[:ctx/elapsed-time [cdq.create.elapsed-time/create]]
    [:ctx/stage [cdq.create.stage/do!]]

    [:ctx/level [cdq.create.level/create cdq.level.vampire/create]]
    [:ctx/tiled-map [cdq.create.level/tiled-map]]
    [:ctx/start-position [cdq.create.level/start-position]]

    [:ctx/grid [cdq.grid/create]]
    [:ctx/raycaster [cdq.raycaster/create]]
    [:ctx/content-grid [cdq.content-grid/create]]
    [:ctx/explored-tile-corners [cdq.create.explored-tile-corners/create]]
    [:ctx/id-counter [cdq.create.id-counter/create]]
    [:ctx/entity-ids [cdq.create.entity-ids/create]]
    [:ctx/potential-field-cache [cdq.create.potential-field-cache/create]]
    cdq.create.spawn-enemies/do!
    [:ctx/player-eid [cdq.create.player-entity/do!]]])

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

(defn reset-game-state! []
 (swap! state create-ctx! create-game-state))

(comment
  (clojure.pprint/pprint (sort (keys @state)))
 )

(defn -main []
  (let [initial-context {:ctx/pausing? true
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
                         :ctx/z-orders ctx/z-orders}
        create-fns (concat create-app-state
                           create-game-state)
        dispose-fn 'cdq.application.dispose/do!
        resize-fn 'cdq.application.resize/do!
        render-fns '[cdq.render.bind-active-entities/do!
                     cdq.render.set-camera-on-player/do!
                     cdq.render.clear-screen/do!
                     cdq.render.draw-tiled-map/do!
                     cdq.render.draw-on-world-viewport/do!
                     cdq.render.draw-ui/do!
                     cdq.render.update-ui/do!
                     cdq.render.player-state-handle-click/do!
                     cdq.render.update-mouseover-entity/do!
                     cdq.render.bind-paused/do!
                     cdq.render.when-not-paused/do!
                     cdq.render.remove-destroyed-entities/do! ; do not pause as pickup item should be destroyed
                     cdq.render.camera-controls/do!]]
    (application/start! {:title "Cyber Dungeon Quest"
                         :window-width 1440
                         :window-height 900
                         :fps 60
                         :dock-icon "moon.png"
                         :create!
                         (fn []
                           (reset! state (create-ctx! initial-context create-fns)))

                         :dispose!
                         (fn []
                           ((requiring-resolve dispose-fn) @state))

                         :render!
                         (fn []
                           (swap! state render-ctx! render-fns))

                         :resize!
                         (fn [_width _height]
                           ((requiring-resolve resize-fn) @state))})))
