(ns cdq.game
  (:require [cdq.app]
            [cdq.audio]
            [cdq.assets]
            [cdq.c]
            [cdq.create.db]
            [cdq.create.ui]
            [cdq.ctx]
            [cdq.dev.data-view :as data-view]
            [cdq.graphics :as graphics]
            [cdq.grid :as grid]
            [cdq.input :as input]
            [cdq.malli :as m]
            [cdq.ui.stage :as stage]
            [qrecord.core :as q])
  (:import (com.badlogic.gdx.utils Disposable)))

(q/defrecord Context [ctx/app
                      ctx/config
                      ctx/input
                      ctx/db
                      ctx/audio
                      ctx/stage
                      ctx/graphics
                      ctx/world])

(defn create! [{:keys [audio files graphics input]}]
  (let [graphics (graphics/create
                  graphics
                  files
                  {:colors [["PRETTY_NAME" [0.84 0.8 0.52 1]]]
                   :textures (cdq.assets/search files
                                                {:folder "resources/"
                                                 :extensions #{"png" "bmp"}})
                   :tile-size 48
                   :ui-viewport    {:width 1440 :height 900}
                   :world-viewport {:width 1440 :height 900}
                   :cursor-path-format "cursors/%s.png"
                   :cursors {:cursors/bag                   ["bag001"       [0   0]]
                             :cursors/black-x               ["black_x"      [0   0]]
                             :cursors/default               ["default"      [0   0]]
                             :cursors/denied                ["denied"       [16 16]]
                             :cursors/hand-before-grab      ["hand004"      [4  16]]
                             :cursors/hand-before-grab-gray ["hand004_gray" [4  16]]
                             :cursors/hand-grab             ["hand003"      [4  16]]
                             :cursors/move-window           ["move002"      [16 16]]
                             :cursors/no-skill-selected     ["denied003"    [0   0]]
                             :cursors/over-button           ["hand002"      [0   0]]
                             :cursors/sandclock             ["sandclock"    [16 16]]
                             :cursors/skill-not-usable      ["x007"         [0   0]]
                             :cursors/use-skill             ["pointer004"   [0   0]]
                             :cursors/walking               ["walking"      [16 16]]}
                   :default-font {:file "exocet/films.EXL_____.ttf"
                                  :params {:size 16
                                           :quality-scaling 2
                                           :enable-markup? true
                                           ; false, otherwise scaling to world-units not visible
                                           :use-integer-positions? false}}})
        ctx (map->Context {:app (cdq.app/create
                                 {:schema [:map {:closed true}
                                           [:ctx/app :some]
                                           [:ctx/config :some]
                                           [:ctx/input :some]
                                           [:ctx/db :some]
                                           [:ctx/audio :some]
                                           [:ctx/stage :some]
                                           [:ctx/graphics :some]
                                           [:ctx/world :some]]
                                  :stacktraces {:print-level 3
                                                :print-depth 24}})
                           :audio (cdq.audio/create audio files {:sounds "sounds.edn"})
                           :config {:cdq.ctx.game/enemy-components {:entity/fsm {:fsm :fsms/npc
                                                                                 :initial-state :npc-sleeping}
                                                                    :entity/faction :evil}
                                    :cdq.ctx.game/player-props {:creature-id :creatures/vampire
                                                                :components {:entity/fsm {:fsm :fsms/player
                                                                                          :initial-state :player-idle}
                                                                             :entity/faction :good
                                                                             :entity/player? true
                                                                             :entity/free-skill-points 3
                                                                             :entity/clickable {:type :clickable/player}
                                                                             :entity/click-distance-tiles 1.5}}
                                    :cdq.ctx.game/world {:content-grid-cell-size 16
                                                         :potential-field-factions-iterations {:good 15
                                                                                               :evil 5}}
                                    :effect-body-props {:width 0.5
                                                        :height 0.5
                                                        :z-order :z-order/effect}

                                    :controls {:zoom-in :minus
                                               :zoom-out :equals
                                               :unpause-once :p
                                               :unpause-continously :space}}
                           :db (cdq.create.db/do! {:schemas "schema.edn"
                                                   :properties "properties.edn"})
                           :graphics graphics
                           :input input
                           :stage (cdq.create.ui/do! graphics input {:skin-scale :x1})})]
    (-> ctx
        (cdq.ctx/reset-game-state! [(requiring-resolve 'cdq.level.from-tmx/create)
                                    {:tmx-file "maps/vampire.tmx"
                                     :start-position [32 71]}])
        cdq.app/validate)))

; TODO call dispose! on all components
(defn dispose! [{:keys [ctx/audio
                        ctx/graphics
                        ctx/world]}]
  (Disposable/.dispose audio)
  (Disposable/.dispose graphics)
  (Disposable/.dispose (:world/tiled-map world))
  ; TODO vis-ui dispose
  ; TODO what else disposable?
  ; => :ctx/tiled-map definitely and also dispose when re-creting gamestate.
  )

; TODO call resize! on all components
(defn resize! [{:keys [ctx/graphics]} width height]
  (graphics/resize-viewports! graphics width height))

(defn- check-open-debug-data-view!
  [{:keys [ctx/input
           ctx/stage
           ctx/world]
    :as ctx}]
  (when (input/button-just-pressed? input :right)
    (let [mouseover-eid (:world/mouseover-eid world)
          data (or (and mouseover-eid @mouseover-eid)
                   @(grid/cell (:world/grid world)
                               (mapv int (cdq.c/world-mouse-position ctx))))]
      (stage/add! stage (data-view/table-view-window {:title "Data View"
                                                      :data data
                                                      :width 500
                                                      :height 500}))))
  ctx)

(defn render! [ctx]
  (reduce (fn [ctx f] (f ctx))
          (-> ctx
              cdq.app/validate
              cdq.app/run-runnables!
              check-open-debug-data-view! ; TODO FIXME its not documented I forgot rightclick can open debug data view!
              )
          (map requiring-resolve
               '[
                 cdq.render.assoc-active-entities/do!
                 cdq.render.set-camera-on-player/do!
                 cdq.render.clear-screen/do!
                 cdq.render.draw-world-map/do!
                 cdq.render.draw-on-world-viewport/do!
                 cdq.render.stage/do!
                 cdq.render.set-cursor/do!
                 cdq.render.player-state-handle-input/do!
                 cdq.render.update-mouseover-entity/do!
                 cdq.render.assoc-paused/do!
                 cdq.render.tick-world/do!
                 cdq.render.remove-destroyed-entities/do! ; do not pause as pickup item should be destroyed
                 cdq.render.camera-controls/do!
                 cdq.render.check-window-hotkeys/do!
                 cdq.app/validate])))
