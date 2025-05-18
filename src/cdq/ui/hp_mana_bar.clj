(ns cdq.ui.hp-mana-bar
  (:require [cdq.ctx :as ctx]
            [cdq.draw :as draw]
            [cdq.entity :as entity]
            [cdq.graphics :as graphics]
            [cdq.utils :as utils]
            [cdq.val-max :as val-max]
            [gdl.ui :as ui]))

(defn- render-infostr-on-bar [infostr x y h]
  (draw/text {:text infostr
              :x (+ x 75)
              :y (+ y 2)
              :up? true}))

(defn create [[x y-mana]]
  (let [rahmen      (graphics/sprite (ctx/assets "images/rahmen.png"))
        hpcontent   (graphics/sprite (ctx/assets "images/hp.png"))
        manacontent (graphics/sprite (ctx/assets "images/mana.png"))
        [rahmenw rahmenh] (:pixel-dimensions rahmen)
        y-hp (+ y-mana rahmenh)
        render-hpmana-bar (fn [x y contentimage minmaxval name]
                            (draw/image rahmen [x y])
                            (draw/image (graphics/sub-sprite contentimage
                                                             [0 0 (* rahmenw (val-max/ratio minmaxval)) rahmenh])
                                        [x y])
                            (render-infostr-on-bar (str (utils/readable-number (minmaxval 0)) "/" (minmaxval 1) " " name) x y rahmenh))]
    (ui/actor
     {:draw (fn [_this]
              (let [player-entity @ctx/player-eid
                    x (- x (/ rahmenw 2))]
                (render-hpmana-bar x y-hp   hpcontent   (entity/hitpoints player-entity) "HP")
                (render-hpmana-bar x y-mana manacontent (entity/mana      player-entity) "MP")))})))
