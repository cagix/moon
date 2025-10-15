(ns cdq.ui.editor.schema
  (:require [cdq.graphics :as graphics]
            [cdq.ui.build.table :as table]
            [clojure.gdx.scene2d.actor :as actor]
            [clojure.scene2d.vis-ui.image-button :as image-button]
            [clojure.utils :as utils]
            [clojure.scene2d.vis-ui.check-box :as check-box]
            [clojure.vis-ui.label :as label]))

(defmulti create (fn [[schema-k :as _schema] v ctx]
                   schema-k))

(defmulti value (fn [[schema-k :as _schema] widget schemas]
                  schema-k))

(defmethod create :default
  [_ v _ctx]
  (label/create (utils/truncate (utils/->edn-str v) 60)))

(defmethod value :default
  [_  widget _schemas]
  ((actor/user-object widget) 1))

(defmethod create :s/animation
  [_ animation {:keys [ctx/graphics]}]
  (table/create
   {:rows [(for [image (:animation/frames animation)]
             {:actor (image-button/create
                      {:drawable/texture-region (graphics/texture-region graphics image)
                       :drawable/scale 2})})]
    :cell-defaults {:pad 1}}))

(defmethod create :s/boolean
  [_ checked? _ctx]
  (assert (boolean? checked?))
  (check-box/create
   :text ""
   :on-clicked (fn [_])
   :checked? checked?))

(defmethod value :s/boolean
  [_ widget _schemas]
  (check-box/checked? widget))
