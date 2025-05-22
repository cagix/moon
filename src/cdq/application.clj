(ns cdq.application
  (:require [cdq.malli :as m]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [gdl.graphics.viewport :as viewport]
            [gdl.utils])
  (:import (com.badlogic.gdx ApplicationAdapter)))

(def application-configuration {:title "Cyber Dungeon Quest"
                                :windowed-mode {:width 1440
                                                :height 900}
                                :foreground-fps 60
                                :mac-os {:glfw-async? true
                                         :dock-icon "moon.png"}})

; TODO do also for entities ...

(def ctx-schema (m/schema [:map {:closed true}

                           ; missing gdx app, input, graphics, audio ?
                           ; => abstract from the whole plattform itself ??
                           ; => move into 1 context ?

                           ; config ?
                           [:ctx/pausing? :some]
                           [:ctx/zoom-speed :some]
                           [:ctx/controls :some]
                           [:ctx/sound-path-format :some]
                           [:ctx/effect-body-props :some]

                           [:ctx/config :some]

                           ; db
                           [:ctx/db :some]

                           ; sounds & textures
                           [:ctx/assets :some]
                           ; * dispose
                           ; * play sound
                           ; * sprites

                           ; graphics
                           [:ctx/batch :some]
                           ; usage:
                           ; * dispose
                           ; * create shape-drawer/stage/tiled-map-renderer
                           ; * cdq.draw
                           ; * draw on world-viewport
                           [:ctx/shape-drawer-texture :some]
                           ; * only dispose / shape-drawer
                           [:ctx/shape-drawer :some]
                           ; * draw
                           [:ctx/unit-scale :some]
                           [:ctx/world-unit-scale :some]
                           [:ctx/cursors :some]
                           [:ctx/default-font :some]
                           [:ctx/world-viewport :some]
                           [:ctx/get-tiled-map-renderer :some]
                           [:ctx/ui-viewport :some]

                           ; ui
                           [:ctx/stage :some]

                           ; game logic:

                           ; time
                           [:ctx/elapsed-time :some]
                           [:ctx/delta-time {:optional true} number?] ; optional - added in render each frame
                           [:ctx/paused? {:optional true} :boolean] ; optional - added in render each frame

                           [:ctx/level :some] ; < - move together ?
                           [:ctx/tiled-map :some]
                           [:ctx/start-position :some]

                           ; < - comes from level/tiled-map
                           [:ctx/grid :some]
                           [:ctx/raycaster :some]
                           [:ctx/content-grid :some]
                           [:ctx/explored-tile-corners :some]

                           ;
                           [:ctx/id-counter :some]
                           [:ctx/entity-ids :some]
                           [:ctx/potential-field-cache :some]
                           ;

                           ; control pointers:
                           [:ctx/mouseover-eid :any]
                           [:ctx/player-eid :some]
                           [:ctx/active-entities {:optional true} :some] ; optional - added in render each frame
                           ]))

(comment

 (m/validate-humanize ctx-schema @state)
 ; delta-time missing required key
 ; mouseover-eid unknown error (can be nil)

 )

(defn check-validity [ctx]
  (m/validate-humanize ctx-schema ctx))

(def initial-context {:ctx/pausing? true
                      :ctx/zoom-speed 0.025
                      :ctx/controls {:zoom-in :minus
                                     :zoom-out :equals
                                     :unpause-once :p
                                     :unpause-continously :space}
                      :ctx/sound-path-format "sounds/%s.wav"
                      :ctx/unit-scale 1
                      :ctx/mouseover-eid nil ; needed ?
                      :ctx/effect-body-props {:width 0.5
                                              :height 0.5
                                              :z-order :z-order/effect}})

(def create-app-state '[[:ctx/config [cdq.create.config/create "config.edn"]]
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

(def create-game-state '[[:ctx/elapsed-time [cdq.create.elapsed-time/create]]
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

(def create-initial-state (concat create-app-state
                                  create-game-state))

; TODO

; * startup slower -> maybe serialized require problem ?
; => beceause so many namespaces ?!

; * fix dev-menu start w. different world (assoc in config !?)
; => 'create' itself pass a config opts around ? idk.

; * click sometimes not working! ( stage & input click handler outside of stage is catched ?)
;  -> comment out stage & check
; => issue disappeared after restart (mac os problem?)

; TODO stage has outdated context as input listener ! set to nil and see ?!
; handle stage different ?

(defn- create-into! [initial-context create-fns]
  (reduce (fn [ctx create-fn]
            (if (vector? create-fn)
              (let [[k [f & params]] create-fn]
                (assoc ctx k (apply (requiring-resolve f) ctx params)))
              (do
               ((requiring-resolve create-fn) ctx)
               ctx)))
          initial-context
          create-fns))

(defn create! []
  (create-into! initial-context create-initial-state))

(def render-fns '[cdq.render.bind-active-entities/do!
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
                  cdq.render.camera-controls/do!])

(defn render! [ctx]
  (reduce (fn [ctx render-fn]
            (if-let [result ((requiring-resolve render-fn) ctx)]
              result
              ctx))
          ctx
          render-fns))

(defn dispose! [{:keys [ctx/assets
                        ctx/batch
                        ctx/shape-drawer-texture
                        ctx/cursors
                        ctx/default-font]}]
  (gdl.utils/dispose! assets)
  (gdl.utils/dispose! batch)
  (gdl.utils/dispose! shape-drawer-texture)
  (run! gdl.utils/dispose! (vals cursors))
  (gdl.utils/dispose! default-font)
  ; TODO vis-ui dispose
  ; TODO dispose world tiled-map/level resources?
  )

(defn resize! [{:keys [ctx/ui-viewport
                       ctx/world-viewport]}]
  (viewport/update! ui-viewport)
  (viewport/update! world-viewport))

(def state (atom nil))

(comment
 (spit "state.clj"
       (with-out-str
        (clojure.pprint/pprint
         (sort (keys @state)))))
 )

(defn reset-game-state! []
  (swap! state create-into! create-game-state))

(defn -main []
  (lwjgl/application application-configuration
                     (proxy [ApplicationAdapter] []
                       (create []
                         (reset! state (create!))
                         (check-validity @state))

                       (dispose []
                         (check-validity @state)
                         (dispose! @state))

                       (render []
                         (check-validity @state)
                         (swap! state render!)
                         (check-validity @state))

                       (resize [_width _height]
                         (check-validity @state)
                         (resize! @state)))))
