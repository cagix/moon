(ns forge.entity.state
  (:require [clojure.utils :refer [defsystem]]))

(defsystem enter)
(defmethod enter :default [_])

(defsystem exit)
(defmethod exit :default [_])

(defsystem cursor)
(defmethod cursor :default [_])

(defsystem manual-tick)
(defmethod manual-tick :default [_])

(defsystem pause-game?)
(defmethod pause-game? :default [_])

(defsystem draw-gui-view [_])
(defmethod draw-gui-view :default [_])

(defsystem clicked-inventory-cell [_ cell])
(defmethod clicked-inventory-cell :default [_ cell])

(defsystem clicked-skillmenu-skill [_ skill])
(defmethod clicked-skillmenu-skill :default [_ skill])

#_(def ^:private entity-state
    (merge-with concat
                entity
                {:optional [#'state-enter
                            #'state-exit
                            #'state-cursor
                            #'pause-game?
                            #'manual-tick
                            #'clicked-inventory-cell
                            #'clicked-skillmenu-skill
                            #'draw-gui-view]}))
