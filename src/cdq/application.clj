(ns cdq.application
  (:require cdq.ctx.create.db
            cdq.ctx.create.graphics
            cdq.ctx.create.stage
            cdq.ctx.create.audio
            cdq.ctx.create.input
            cdq.ctx.create.world
            cdq.info-impl
            clojure.scene2d.builds
            cdq.scene2d.build.editor-overview-window
            cdq.scene2d.build.editor-window
            cdq.scene2d.build.map-widget-table
            clojure.scene2d.build.actor
            clojure.scene2d.build.group
            clojure.scene2d.build.horizontal-group
            clojure.scene2d.build.scroll-pane
            clojure.scene2d.build.separator-horizontal
            clojure.scene2d.build.separator-vertical
            clojure.scene2d.build.stack
            clojure.scene2d.build.widget
            cdq.ui.actor-information
            cdq.ui.error-window

            [cdq.audio :as audio]

            [cdq.graphics :as graphics]
            [cdq.graphics.draws :as draws]
            [cdq.graphics.camera :as camera]
            [cdq.graphics.textures :as textures]
            [cdq.graphics.tiled-map-renderer :as tiled-map-renderer]
            [cdq.graphics.ui-viewport :as ui-viewport]
            [cdq.graphics.world-viewport :as world-viewport]
            [clojure.gdx.graphics.color :as color]

            [cdq.input :as input]

            [clojure.scene2d.vis-ui :as vis-ui]
            [cdq.ui :as ui]
            [cdq.ui.stage :as stage]
            [clojure.scene2d :as scene2d]

            [cdq.entity.state :as state]
            [cdq.entity.body :as body]
            [cdq.entity.skills.skill :as skill]
            [cdq.world :as world]
            [cdq.world.content-grid :as content-grid]
            [cdq.world.grid :as grid]
            [cdq.world.raycaster :as raycaster]
            [clojure.math.vector2 :as v]

            [clojure.tx-handler :as tx-handler]
            [clojure.txs :as txs]
            [clojure.throwable :as throwable]
            [clojure.info :as info]

            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.utils :as utils]
            [malli.core :as m]
            [malli.utils :as mu]
            [qrecord.core :as q])
  (:import (com.badlogic.gdx ApplicationListener
                             Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (org.lwjgl.system Configuration))
  (:gen-class))

(defn render-stage
  [{:keys [ctx/stage]
    :as ctx}]
  (stage/set-ctx! stage ctx)
  (stage/act!     stage)
  (stage/draw!    stage)
  (stage/get-ctx  stage))

(def zoom-speed 0.025)

(defn window-camera-controls
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage]
    :as ctx}]
  (when (input/zoom-in?            input) (camera/change-zoom! graphics zoom-speed))
  (when (input/zoom-out?           input) (camera/change-zoom! graphics (- zoom-speed)))
  (when (input/close-windows?      input) (ui/close-all-windows!         stage))
  (when (input/toggle-inventory?   input) (ui/toggle-inventory-visible!  stage))
  (when (input/toggle-entity-info? input) (ui/toggle-entity-info-window! stage))
  ctx)

(def destroy-components
  {:entity/destroy-audiovisual
   {:destroy! (fn [audiovisuals-id eid _ctx]
                [[:tx/audiovisual
                  (:body/position (:entity/body @eid))
                  audiovisuals-id]])}})

(defn remove-destroyed-entities
  [{:keys [ctx/world]
    :as ctx}]
  (let [{:keys [world/content-grid
                world/entity-ids
                world/grid]} world]
    (doseq [eid (filter (comp :entity/destroyed? deref)
                        (vals @entity-ids))]
      (let [id (:entity/id @eid)]
        (assert (contains? @entity-ids id))
        (swap! entity-ids dissoc id))
      (content-grid/remove-entity! content-grid eid)
      (grid/remove-from-touched-cells! grid eid)
      (when (:body/collides? (:entity/body @eid))
        (grid/remove-from-occupied-cells! grid eid))
      (txs/handle! ctx
                   (mapcat (fn [[k v]]
                             (when-let [destroy! (:destroy! (k destroy-components))]
                               (destroy! v eid ctx)))
                           @eid))))
  ctx)

(defn tick-entities
  [{:keys [ctx/stage
           ctx/world]
    :as ctx}]
  (if (:world/paused? world)
    ctx
    (do (try
         (txs/handle! ctx (world/tick-entities! world))
         (catch Throwable t
           (throwable/pretty-pst t)
           (ui/show-error-window! stage t)))
        ctx)))

(defn- player-effect-ctx [mouseover-eid world-mouse-position player-eid]
  (let [target-position (or (and mouseover-eid
                                 (:body/position (:entity/body @mouseover-eid)))
                            world-mouse-position)]
    {:effect/source player-eid
     :effect/target mouseover-eid
     :effect/target-position target-position
     :effect/target-direction (v/direction (:body/position (:entity/body @player-eid))
                                           target-position)}))

(defn- interaction-state
  [stage
   world-mouse-position
   mouseover-eid
   player-eid
   mouseover-actor]
  (cond
   mouseover-actor
   [:interaction-state/mouseover-actor (ui/actor-information stage mouseover-actor)]

   (and mouseover-eid
        (:entity/clickable @mouseover-eid))
   [:interaction-state/clickable-mouseover-eid
    {:clicked-eid mouseover-eid
     :in-click-range? (< (body/distance (:entity/body @player-eid)
                                        (:entity/body @mouseover-eid))
                         (:entity/click-distance-tiles @player-eid))}]

   :else
   (if-let [skill-id (ui/action-bar-selected-skill stage)]
     (let [entity @player-eid
           skill (skill-id (:entity/skills entity))
           effect-ctx (player-effect-ctx mouseover-eid world-mouse-position player-eid)
           state (skill/usable-state skill entity effect-ctx)]
       (if (= state :usable)
         [:interaction-state.skill/usable [skill effect-ctx]]
         [:interaction-state.skill/not-usable state]))
     [:interaction-state/no-skill-selected])))

(defn assoc-interaction-state
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage
           ctx/world]
    :as ctx}]
  (assoc ctx :ctx/interaction-state (interaction-state stage
                                                       (:graphics/world-mouse-position graphics)
                                                       (:world/mouseover-eid world)
                                                       (:world/player-eid    world)
                                                       (ui/mouseover-actor stage (input/mouse-position input)))))

(def ^:private schema
  (m/schema
   [:map {:closed true}
    [:ctx/audio :some]
    [:ctx/db :some]
    [:ctx/gdx :some]
    [:ctx/graphics :some]
    [:ctx/input :some]
    [:ctx/stage :some]
    [:ctx/actor-fns :some]
    [:ctx/world :some]]))

(defn- validate [ctx]
  (mu/validate-humanize schema ctx)
  ctx)

(q/defrecord Context [])

(defn- player-add-skill!
  [{:keys [ctx/graphics
           ctx/stage]}
   skill]
  (ui/add-skill! stage
                 {:skill-id (:property/id skill)
                  :texture-region (textures/texture-region graphics (:entity/image skill))
                  :tooltip-text (fn [{:keys [ctx/world]}]
                                  (info/text skill world))})
  nil)

(defn- player-set-item!
  [{:keys [ctx/graphics
           ctx/stage]}
   cell item]
  (ui/set-item! stage cell
                {:texture-region (textures/texture-region graphics (:entity/image item))
                 :tooltip-text (info/text item nil)})
  nil)

(defn player-remove-item! [{:keys [ctx/stage]}
                           cell]
  (ui/remove-item! stage cell)
  nil)

(defn toggle-inventory-visible! [{:keys [ctx/stage]}]
  (ui/toggle-inventory-visible! stage)
  nil)

(defn show-message! [{:keys [ctx/stage]} message]
  (ui/show-text-message! stage message)
  nil)

(defn show-modal! [{:keys [ctx/stage]} opts]
  (ui/show-modal-window! stage (stage/viewport stage) opts)
  nil)

(def ^:private txs-fn-map
  '{
    :tx/assoc (fn [_ctx eid k value]
                (swap! eid assoc k value)
                nil)
    :tx/assoc-in (fn [_ctx eid ks value]
                   (swap! eid assoc-in ks value)
                   nil)
    :tx/dissoc (fn [_ctx eid k]
                 (swap! eid dissoc k)
                 nil)
    :tx/update (fn [_ctx eid & params]
                 (apply swap! eid update params)
                 nil)
    :tx/mark-destroyed (fn [_ctx eid]
                         (swap! eid assoc :entity/destroyed? true)
                         nil)
    :tx/set-cooldown cdq.tx.set-cooldown/do!
    :tx/add-text-effect cdq.tx.add-text-effect/do!
    :tx/add-skill cdq.tx.add-skill/do!
    :tx/set-item cdq.tx.set-item/do!
    :tx/remove-item cdq.tx.remove-item/do!
    :tx/pickup-item cdq.tx.pickup-item/do!
    :tx/event cdq.tx.event/do!
    :tx/state-exit cdq.tx.state-exit/do!
    :tx/state-enter cdq.tx.state-enter/do!
    :tx/effect cdq.tx.effect/do!
    :tx/audiovisual cdq.tx.audiovisual/do!
    :tx/spawn-alert cdq.tx.spawn-alert/do!
    :tx/spawn-line cdq.tx.spawn-line/do!
    :tx/move-entity cdq.tx.move-entity/do!
    :tx/spawn-projectile cdq.tx.spawn-projectile/do!
    :tx/spawn-effect cdq.tx.spawn-effect/do!
    :tx/spawn-item     cdq.tx.spawn-item/do!
    :tx/spawn-creature cdq.tx.spawn-creature/do!
    :tx/spawn-entity   cdq.tx.spawn-entity/do!

    :tx/sound (fn [{:keys [ctx/audio]} sound-name]
                (audio/play! audio sound-name)
                nil)
    :tx/toggle-inventory-visible cdq.application/toggle-inventory-visible!
    :tx/show-message             cdq.application/show-message!
    :tx/show-modal               cdq.application/show-modal!
    }
  )

(alter-var-root #'txs-fn-map update-vals
                (fn [form]
                  (if (symbol? form)
                    (let [avar (requiring-resolve form)]
                      (assert avar form)
                      avar)
                    (eval form))))

(def ^:private reaction-txs-fn-map
  {

   :tx/set-item (fn [ctx eid cell item]
                  (when (:entity/player? @eid)
                    (player-set-item! ctx cell item)
                    nil))

   :tx/remove-item (fn [ctx eid cell]
                     (when (:entity/player? @eid)
                       (player-remove-item! ctx cell)
                       nil))

   :tx/add-skill (fn [ctx eid skill]
                   (when (:entity/player? @eid)
                     (player-add-skill! ctx skill)
                     nil))
   }
  )

(extend-type Context
  txs/TransactionHandler
  (handle! [ctx txs]
    (let [handled-txs (tx-handler/actions! txs-fn-map
                                           ctx
                                           txs)]
      (tx-handler/actions! reaction-txs-fn-map
                           ctx
                           handled-txs
                           :strict? false))))

(defn pipeline [ctx pipeline]
  (reduce (fn [ctx [f & args]]
            (apply f ctx args))
          ctx
          pipeline))

(defn- create! []
  (vis-ui/load! {:skin-scale :x1})
  (pipeline {:ctx/gdx {:clojure.gdx/audio    Gdx/audio
                       :clojure.gdx/files    Gdx/files
                       :clojure.gdx/graphics Gdx/graphics
                       :clojure.gdx/input    Gdx/input}}
            [[(fn [ctx]
                (merge (map->Context {})
                       ctx))]
             [cdq.ctx.create.db/do!]
             [cdq.ctx.create.graphics/do! {:tile-size 48
                                           :ui-viewport {:width 1440
                                                         :height 900}
                                           :world-viewport {:width 1440
                                                            :height 900}
                                           :texture-folder {:folder "resources/"
                                                            :extensions #{"png" "bmp"}}
                                           :default-font {:path "exocet/films.EXL_____.ttf"
                                                          :params {:size 16
                                                                   :quality-scaling 2
                                                                   :enable-markup? true
                                                                   :use-integer-positions? false
                                                                   ; :texture-filter/linear because scaling to world-units
                                                                   :min-filter :linear
                                                                   :mag-filter :linear}}
                                           :colors {"PRETTY_NAME" [0.84 0.8 0.52 1]}
                                           :cursors {:path-format "cursors/%s.png"
                                                     :data {:cursors/bag                   ["bag001"       [0   0]]
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
                                                            :cursors/walking               ["walking"      [16 16]]}}}]
             [cdq.ctx.create.stage/do! '[[cdq.ctx.create.ui.dev-menu/create cdq.ctx.create.world/do!]
                                         [cdq.ctx.create.ui.action-bar/create]
                                         [cdq.ctx.create.ui.hp-mana-bar/create]
                                         [cdq.ctx.create.ui.windows/create [[cdq.ctx.create.ui.windows.entity-info/create]
                                                                            [cdq.ctx.create.ui.windows.inventory/create]]]
                                         [cdq.ctx.create.ui.player-state-draw/create]
                                         [cdq.ctx.create.ui.message/create]]]
             [cdq.ctx.create.input/do!]
             [cdq.ctx.create.audio/do!]
             [cdq.ctx.create.world/do! "world_fns/vampire.edn"]]))

(defn- resize! [{:keys [ctx/graphics]} width height]
  (ui-viewport/update!    graphics width height)
  (world-viewport/update! graphics width height))

(defn- dispose!
  [{:keys [ctx/audio
           ctx/graphics
           ctx/world]}]
  (vis-ui/dispose!)
  (audio/dispose! audio)
  (graphics/dispose! graphics)
  (world/dispose! world))

(defn- get-stage-ctx
  [{:keys [ctx/stage]
    :as ctx}]
  (or (ui/get-ctx stage)
      ctx)) ; first render stage does not have ctx set.

(defn- update-mouse
  [{:keys [ctx/graphics
           ctx/input]
    :as ctx}]
  (let [mp (input/mouse-position input)]
    (-> ctx
        (assoc-in [:ctx/graphics :graphics/world-mouse-position] (world-viewport/unproject graphics mp))
        (assoc-in [:ctx/graphics :graphics/ui-mouse-position   ] (ui-viewport/unproject    graphics mp))
        )))

(defn- get-mouseover-entity
  [{:keys [world/grid
           world/mouseover-eid
           world/player-eid
           world/render-z-order]
    :as world}
   position]
  (let [player @player-eid
        hits (remove #(= (:body/z-order (:entity/body @%)) :z-order/effect)
                     (grid/point->entities grid position))]
    (->> render-z-order
         (utils/sort-by-order hits #(:body/z-order (:entity/body @%)))
         reverse
         (filter #(raycaster/line-of-sight? world player @%))
         first)))

(defn- update-mouseover-eid
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage
           ctx/world]
    :as ctx}]
  (let [mouseover-actor (ui/mouseover-actor stage (input/mouse-position input))
        mouseover-eid (:world/mouseover-eid world)
        new-eid (if mouseover-actor
                  nil
                  (get-mouseover-entity world (:graphics/world-mouse-position graphics)))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc-in ctx [:ctx/world :world/mouseover-eid] new-eid)))

(defn- check-open-debug
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage
           ctx/world]
    :as ctx}]
  (when (input/open-debug-button-pressed? input)
    (let [data (or (and (:world/mouseover-eid world) @(:world/mouseover-eid world))
                   @((:world/grid world) (mapv int (:graphics/world-mouse-position graphics))))]
      (ui/show-data-viewer! stage data)))
  ctx)

(defn- assoc-active-entities
  [{:keys [ctx/world]
    :as ctx}]
  (update ctx :ctx/world world/cache-active-entities))

(defn- set-camera-on-player!
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (camera/set-position! graphics
                        (:body/position
                         (:entity/body
                          @(:world/player-eid world))))
  ctx)

(defn- clear-screen!
  [{:keys [ctx/graphics] :as ctx}]
  (graphics/clear! graphics color/black)
  ctx)

(defn- tile-color-setter
  [{:keys [ray-blocked?
           explored-tile-corners
           light-position
           see-all-tiles?
           explored-tile-color
           visible-tile-color
           invisible-tile-color]}]
  #_(reset! do-once false)
  (let [light-cache (atom {})]
    (fn tile-color-setter [_color x y]
      (let [position [(int x) (int y)]
            explored? (get @explored-tile-corners position) ; TODO needs int call ?
            base-color (if explored?
                         explored-tile-color
                         invisible-tile-color)
            cache-entry (get @light-cache position :not-found)
            blocked? (if (= cache-entry :not-found)
                       (let [blocked? (ray-blocked? light-position position)]
                         (swap! light-cache assoc position blocked?)
                         blocked?)
                       cache-entry)]
        #_(when @do-once
            (swap! ray-positions conj position))
        (if blocked?
          (if see-all-tiles?
            visible-tile-color
            base-color)
          (do (when-not explored?
                (swap! explored-tile-corners assoc (mapv int position) true))
              visible-tile-color))))))

(comment
 (def ^:private count-rays? false)

 (def ray-positions (atom []))
 (def do-once (atom true))

 (count @ray-positions)
 2256
 (count (distinct @ray-positions))
 608
 (* 608 4)
 2432
 )

(defn draw-world-map!
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (tiled-map-renderer/draw! graphics
                            (:world/tiled-map world)
                            (tile-color-setter
                             {:ray-blocked? (partial raycaster/blocked? world)
                              :explored-tile-corners (:world/explored-tile-corners world)
                              :light-position (camera/position graphics)
                              :see-all-tiles? false
                              :explored-tile-color  [0.5 0.5 0.5 1]
                              :visible-tile-color   [1 1 1 1]
                              :invisible-tile-color [0 0 0 1]}))
  ctx)

(defn- draw-on-world-viewport!
  [{:keys [ctx/graphics]
    :as ctx}
   draw-fns]
  (world-viewport/draw! graphics
                        (fn []
                          (doseq [f (map requiring-resolve draw-fns)]
                            (draws/handle! graphics (f ctx)))))
  ctx)

(defn set-cursor!
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (let [eid (:world/player-eid world)
        entity @eid
        state-k (:state (:entity/fsm entity))
        cursor-key (state/cursor [state-k (state-k entity)] eid ctx)]
    (graphics/set-cursor! graphics cursor-key))
  ctx)

(defn player-state-handle-input
  [{:keys [ctx/world]
    :as ctx}]
  (let [eid (:world/player-eid world)
        entity @eid
        state-k (:state (:entity/fsm entity))
        txs (state/handle-input [state-k (state-k entity)] eid ctx)]
    (txs/handle! ctx txs))
  ctx)

(defn dissoc-interaction-state [ctx]
  (dissoc ctx :ctx/interaction-state))

(def pausing? true)

(def state->pause-game? {:stunned false
                         :player-moving false
                         :player-item-on-cursor true
                         :player-idle true
                         :player-dead true
                         :active-skill false})

(defn assoc-paused
  [{:keys [ctx/input
           ctx/world]
    :as ctx}]
  (assoc-in ctx [:ctx/world :world/paused?]
            (or #_error
                (and pausing?
                     (state->pause-game? (:state (:entity/fsm @(:world/player-eid world))))
                     (not (input/unpause? input))))))

(defn- update-world-time* [{:keys [world/max-delta]
                           :as world}
                          delta-ms]
  (let [delta-ms (min delta-ms max-delta)]
    (-> world
        (assoc :world/delta-time delta-ms)
        (update :world/elapsed-time + delta-ms))))

(defn update-world-time
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (if (:world/paused? (:ctx/world ctx))
    ctx
    (update ctx :ctx/world update-world-time* (graphics/delta-time graphics))))

(defn update-potential-fields
  [{:keys [ctx/world]
    :as ctx}]
  (if (:world/paused? world)
    ctx
    (do
     (world/update-potential-fields! world)
     ctx)))

(defn- render! [ctx]
  (pipeline ctx
            [[get-stage-ctx]
             [validate]
             [update-mouse]
             [update-mouseover-eid]
             [check-open-debug]
             [assoc-active-entities]
             [set-camera-on-player!]
             [clear-screen!]
             [draw-world-map!]
             [draw-on-world-viewport! '[cdq.ctx.render.draw-on-world-viewport.draw-tile-grid/do!
                                        cdq.ctx.render.draw-on-world-viewport.draw-cell-debug/do!
                                        cdq.ctx.render.draw-on-world-viewport.draw-entities/do!
                                        #_cdq.ctx.render.draw-on-world-viewport.geom-test/do!
                                        cdq.ctx.render.draw-on-world-viewport.highlight-mouseover-tile/do!]]
             [assoc-interaction-state]
             [set-cursor!]
             [player-state-handle-input]
             [dissoc-interaction-state]
             [assoc-paused]
             [update-world-time]
             [update-potential-fields]
             [tick-entities]
             [remove-destroyed-entities]
             [window-camera-controls]
             [render-stage]
             [validate]]))

(def state (atom nil))

(defn -main []
  (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
  (Lwjgl3Application. (reify ApplicationListener
                        (create [_]
                          (reset! state (create!)))
                        (dispose [_]
                          (dispose! @state))
                        (render [_]
                          (swap! state render!))
                        (resize [_ width height]
                          (resize! @state width height))
                        (pause [_])
                        (resume [_]))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle "Cyber Dungeon Quest")
                        (.setWindowedMode 1440 900)
                        (.setForegroundFPS 60))))

(comment

 (.postRunnable Gdx/app
                (fn []
                  (let [{:keys [ctx/db]
                         :as ctx} @state]
                    (txs/handle! ctx
                                 [[:tx/spawn-creature
                                   {:position [35 73]
                                    :creature-property (db/build db :creatures/dragon-red)
                                    :components {:entity/fsm {:fsm :fsms/npc
                                                              :initial-state :npc-sleeping}
                                                 :entity/faction :evil}}]]))))
 )
