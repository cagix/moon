(ns cdq.editor.widget
  (:require [cdq.schema :as schema]))

(defn- widget-type
  [schema attribute]
  (let [stype (schema/type schema)]
    (cond
     (= attribute :entity/animation)
     :widget/animation

     (= attribute :entity/image)
     :widget/image

     (#{:s/map-optional :s/components-ns} stype)
     :s/map

     (#{:s/number :s/nat-int :s/int :s/pos :s/pos-int :s/val-max} stype)
     :widget/edn

     :else stype)))

(defmulti create (fn [schema attribute v _ctx]
                   (widget-type schema attribute)))

(defmulti value (fn [schema attribute _widget _schemas]
                  (widget-type schema attribute)))
