(ns forge.effects
  (:require [forge.component :refer [defsystem]]))

(defsystem handle [_ ctx])

(defsystem applicable? [_ ctx])

(defsystem useful?          [_  ctx])
(defmethod useful? :default [_ _ctx] true)

(defsystem render           [_  ctx])
(defmethod render :default  [_ _ctx])

(defn *applicable? [ctx effects]
  (seq (filter #(applicable? % ctx) effects)))

(defn *useful? [ctx effects]
  (->> effects
       (*applicable? ctx)
       (some #(useful? % ctx))))

(defn do! [ctx effects]
  (run! #(handle % ctx) (*applicable? ctx effects)))

(defn *render [ctx effects]
  (run! #(render % ctx) effects)) ; TODO applicable?!
