(ns cdq.ui.hp-mana-bar
  (:require [cdq.entity :as entity]
            [cdq.val-max :as val-max]
            [gdl.c :as c]
            [gdl.ui :as ui]
            [gdl.utils :as utils]))

(defn- render-infostr-on-bar [infostr x y h]
  [:draw/text {:text infostr
               :x (+ x 75)
               :y (+ y 2)
               :up? true}])

(defn create [[x y-mana] ctx]
  (let [rahmen      (c/sprite ctx "images/rahmen.png")
        hpcontent   (c/sprite ctx "images/hp.png")
        manacontent (c/sprite ctx "images/mana.png" )
        [rahmenw rahmenh] (:pixel-dimensions rahmen)
        y-hp (+ y-mana rahmenh)
        render-hpmana-bar (fn [ctx x y contentimage minmaxval name]
                            [[:draw/image rahmen [x y]]
                             [:draw/image
                              (c/sub-sprite ctx
                                            contentimage
                                            [0 0 (* rahmenw (val-max/ratio minmaxval)) rahmenh])
                              [x y]]
                             (render-infostr-on-bar (str (utils/readable-number (minmaxval 0))
                                                         "/"
                                                         (minmaxval 1)
                                                         " "
                                                         name)
                                                    x
                                                    y
                                                    rahmenh)])
        create-draws (fn [{:keys [ctx/player-eid] :as ctx}]
                       (let [player-entity @player-eid
                             x (- x (/ rahmenw 2))]
                         (concat
                          (render-hpmana-bar ctx x y-hp   hpcontent   (entity/hitpoints player-entity) "HP")
                          (render-hpmana-bar ctx x y-mana manacontent (entity/mana      player-entity) "MP"))))]
    (ui/actor
     {:draw (fn [_this ctx]
              (c/handle-draws! ctx (create-draws ctx)))})))
