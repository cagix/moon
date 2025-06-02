(ns clojure.ui.hp-mana-bar
  (:require [clojure.ctx :as ctx]
            [clojure.entity :as entity]
            [clojure.val-max :as val-max]
            [clojure.ui :as ui]
            [clojure.utils :as utils]))

(defn- render-infostr-on-bar [infostr x y h]
  [:draw/text {:text infostr
               :x (+ x 75)
               :y (+ y 2)
               :up? true}])

(defn create [{:keys [ctx/ui-viewport]
               :as ctx}]
  (let [[x y-mana] [(/ (:width ui-viewport) 2)
                    80 ; action-bar-icon-size
                    ]
        rahmen      (ctx/sprite ctx "images/rahmen.png")
        hpcontent   (ctx/sprite ctx "images/hp.png")
        manacontent (ctx/sprite ctx "images/mana.png" )
        [rahmenw rahmenh] (:pixel-dimensions rahmen)
        y-hp (+ y-mana rahmenh)
        render-hpmana-bar (fn [ctx x y contentimage minmaxval name]
                            [[:draw/image rahmen [x y]]
                             [:draw/image
                              (ctx/sub-sprite ctx
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
              (ctx/handle-draws! ctx (create-draws ctx)))})))
