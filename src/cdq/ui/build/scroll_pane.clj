(ns cdq.ui.build.scroll-pane
  (:require [clojure.vis-ui.scroll-pane :as scroll-pane]
            [clojure.gdx.scene2d.actor :as actor]))

(defn create
  [{:keys [scroll-pane/actor
           actor/name]}]
  (doto (scroll-pane/create actor)
    (actor/set-name! name)))
