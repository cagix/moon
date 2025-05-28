(ns cdq.application-state
  (:require [cdq.db :as db]
            [cdq.g :as g]
            [cdq.graphics :as graphics]
            [cdq.game-state :as game-state]
            [cdq.ui.dev-menu]
            [cdq.ui.action-bar]
            [cdq.ui.hp-mana-bar]
            [cdq.ui.windows]
            [cdq.ui.entity-info]
            [cdq.ui.inventory]
            [cdq.ui.player-state-draw]
            [cdq.ui.message]
            [gdl.application]
            [gdl.assets :as assets]
            [gdl.input :as input]
            [gdl.ui :as ui]
            [gdl.viewport :as viewport])
  (:import (com.badlogic.gdx Gdx
                             Input$Keys
                             Input$Buttons)))

(defn- button->code [button]
  (case button
    :left Input$Buttons/LEFT
    ))

(defn- k->code [key]
  (case key
    :minus  Input$Keys/MINUS
    :equals Input$Keys/EQUALS
    :space  Input$Keys/SPACE
    :p      Input$Keys/P
    :enter  Input$Keys/ENTER
    :escape Input$Keys/ESCAPE
    :i      Input$Keys/I
    :e      Input$Keys/E
    :d      Input$Keys/D
    :a      Input$Keys/A
    :w      Input$Keys/W
    :s      Input$Keys/S
    ))

(defn- make-input [input]
  (reify input/Input
    (button-just-pressed? [_ button]
      (.isButtonJustPressed input (button->code button)))

    (key-pressed? [_ key]
      (.isKeyPressed input (k->code key)))

    (key-just-pressed? [_ key]
      (.isKeyJustPressed input (k->code key)))))

(defn- add-stage! [ctx]
  (let [stage (ui/stage (:java-object (:ctx/ui-viewport ctx))
                        (:batch (:ctx/graphics ctx)))]
    (.setInputProcessor Gdx/input stage)
    (assoc ctx :ctx/stage stage)))

(defn- create-actors [{:keys [ctx/ui-viewport]
                       :as ctx}]
  [(cdq.ui.dev-menu/create ctx)
   (cdq.ui.action-bar/create :id :action-bar)
   (cdq.ui.hp-mana-bar/create [(/ (:width ui-viewport) 2)
                               80 ; action-bar-icon-size
                               ]
                              ctx)
   (cdq.ui.windows/create :id :windows
                          :actors [(cdq.ui.entity-info/create [(:width ui-viewport) 0])
                                   (cdq.ui.inventory/create ctx
                                                            :id :inventory-window
                                                            :position [(:width ui-viewport)
                                                                       (:height ui-viewport)])])
   (cdq.ui.player-state-draw/create)
   (cdq.ui.message/create :name "player-message")])

(extend-type gdl.application.Context
  g/Stage
  (find-actor-by-name [{:keys [ctx/stage]} name]
    (-> stage
        ui/root
        (ui/find-actor name))) ; <- find-actor protocol & for stage use ui/root

  (mouseover-actor [{:keys [ctx/ui-viewport
                            ctx/stage]}]
    (ui/hit stage (viewport/mouse-position ui-viewport)))

  (reset-actors! [{:keys [ctx/stage] :as ctx}]
    (ui/clear! stage)
    (run! #(ui/add! stage %) (create-actors ctx))))

(defn create! [config]
  (ui/load! (:ui config))
  (-> (gdl.application/map->Context {})
      (assoc :ctx/config config)
      (assoc :ctx/graphics (graphics/create Gdx/graphics config)) ; <- actually create only called here <- all libgdx create stuff here and assets/input/graphics/stage/viewport as protocols in gdl ? -> all gdx code creating together and upfactored protocols?
      (assoc :ctx/input (make-input Gdx/input))
      (assoc :ctx/ui-viewport (viewport/ui-viewport (:ui-viewport config))) ; <- even viewport construction is in here .... viewport itself a protocol  ....
      (add-stage!)
      (assoc :ctx/assets (assets/create (:assets config)))
      (assoc :ctx/db (db/create (:db config)))
      (game-state/create! (:world-fn config))))

(extend-type gdl.application.Context
  g/Graphics
  (sprite [{:keys [ctx/assets] :as ctx} texture-path] ; <- textures should be inside graphics, makes this easier.
    (graphics/sprite (:ctx/graphics ctx)
                     (assets/texture assets texture-path)))

  (sub-sprite [ctx sprite [x y w h]]
    (graphics/sub-sprite (:ctx/graphics ctx)
                         sprite
                         [x y w h]))

  (sprite-sheet [{:keys [ctx/assets] :as ctx} texture-path tilew tileh]
    (graphics/sprite-sheet (:ctx/graphics ctx)
                           (assets/texture assets texture-path)
                           tilew
                           tileh))

  (sprite-sheet->sprite [ctx sprite-sheet [x y]]
    (graphics/sprite-sheet->sprite (:ctx/graphics ctx)
                                   sprite-sheet
                                   [x y])))
