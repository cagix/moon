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
            [cdq.graphics.textures :as textures]
            [cdq.graphics.ui-viewport :as ui-viewport]
            [cdq.graphics.world-viewport :as world-viewport]

            [clojure.scene2d.vis-ui :as vis-ui]
            [cdq.ui :as ui]
            [cdq.ui.stage :as stage]
            [clojure.scene2d :as scene2d]

            [cdq.world :as world]

            [clojure.tx-handler :as tx-handler]
            [clojure.txs :as txs]
            [clojure.info :as info]

            [clojure.edn :as edn]
            [clojure.java.io :as io]

            [qrecord.core :as q])
  (:import (com.badlogic.gdx ApplicationListener
                             Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (org.lwjgl.system Configuration))
  (:gen-class))

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

(def state (atom nil))

(defn pipeline [ctx pipeline]
  (reduce (fn [ctx [f & args]]
            (apply f ctx args))
          ctx
          pipeline))

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

(defn -main []
  (let [app (-> "cdq.application.edn"
                io/resource
                slurp
                edn/read-string)
        req-resolve (fn [sym sym-format]
                      (requiring-resolve (symbol (format sym-format sym))))
        render-pipeline (map #(update % 0 req-resolve "cdq.ctx.render.%s/do!") (:render-pipeline app))]
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
    (Lwjgl3Application. (reify ApplicationListener
                          (create [_]
                            (vis-ui/load! {:skin-scale :x1})
                            (reset! state (pipeline {:ctx/gdx {:clojure.gdx/audio    Gdx/audio
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
                                                     [cdq.ctx.create.world/do! "world_fns/vampire.edn"]])))
                          (dispose [_]
                            (dispose! @state))
                          (render [_]
                            (swap! state pipeline render-pipeline))
                          (resize [_ width height]
                            (resize! @state width height))
                          (pause [_])
                          (resume [_]))
                        (doto (Lwjgl3ApplicationConfiguration.)
                          (.setTitle "Cyber Dungeon Quest")
                          (.setWindowedMode 1440 900)
                          (.setForegroundFPS 60)))))
