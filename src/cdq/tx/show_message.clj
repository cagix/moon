(ns cdq.tx.show-message
  (:require [clojure.gdx.scenes.scene2d.group :as group]
            [clojure.gdx.scenes.scene2d.stage :as stage]
            [cdq.ui.message]))

(defn do!
  [[_ message]
   {:keys [ctx/stage]}]
  (-> stage
      stage/root
      (group/find-actor "player-message")
      (cdq.ui.message/show! message))
  nil)
