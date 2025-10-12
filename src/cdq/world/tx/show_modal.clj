(ns cdq.world.tx.show-modal
  (:require [cdq.ui :as ui]
            [clojure.gdx.scene2d.stage :as stage]))

(defn do! [{:keys [ctx/stage]} opts]
  (ui/show-modal-window! stage (stage/viewport stage) opts)
  nil)
