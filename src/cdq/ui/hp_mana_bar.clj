(ns cdq.ui.hp-mana-bar
  (:require [cdq.ctx :as ctx]
            [cdq.data.val-max :as val-max]
            [cdq.entity :as entity]
            [cdq.graphics :as graphics]
            [clojure.gdx.scene2d.actor :as actor]
            [clojure.utils :as utils]))

(defn- render-infostr-on-bar [g infostr x y h]
  (graphics/draw-text g {:text infostr
                         :x (+ x 75)
                         :y (+ y 2)
                         :up? true}))

(defn create [[x y-mana]]
  (let [rahmen      (graphics/sprite ctx/graphics (ctx/assets "images/rahmen.png"))
        hpcontent   (graphics/sprite ctx/graphics (ctx/assets "images/hp.png"))
        manacontent (graphics/sprite ctx/graphics (ctx/assets "images/mana.png"))
        [rahmenw rahmenh] (:pixel-dimensions rahmen)
        y-hp (+ y-mana rahmenh)
        render-hpmana-bar (fn [g x y contentimage minmaxval name]
                            (graphics/draw-image g rahmen [x y])
                            (graphics/draw-image g (graphics/sub-sprite g
                                                                        contentimage
                                                                        [0 0 (* rahmenw (val-max/ratio minmaxval)) rahmenh])
                                                 [x y])
                            (render-infostr-on-bar g (str (utils/readable-number (minmaxval 0)) "/" (minmaxval 1) " " name) x y rahmenh))]
    (actor/create {:draw (fn [_this]
                           (let [player-entity @ctx/player-eid
                                 x (- x (/ rahmenw 2))
                                 g ctx/graphics]
                             (render-hpmana-bar g x y-hp   hpcontent   (entity/hitpoints player-entity) "HP")
                             (render-hpmana-bar g x y-mana manacontent (entity/mana      player-entity) "MP")))})))
