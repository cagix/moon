(ns forge.schema
  (:refer-clojure :exclude [type]))

(defn type [schema]
  (if (vector? schema)
    (schema 0)
    schema))

(defmulti form type)
(defmethod form :default [schema] schema)

(defmethod form :s/number  [_] number?)
(defmethod form :s/nat-int [_] nat-int?)
(defmethod form :s/int     [_] int?)
(defmethod form :s/pos     [_] pos?)
(defmethod form :s/pos-int [_] pos-int?)

(defmethod form :s/sound [_] :string)

(defmethod form :s/image [_]
  [:map {:closed true}
   [:file :string]
   [:sub-image-bounds {:optional true} [:vector {:size 4} nat-int?]]])

(defmethod form :s/animation [_]
  [:map {:closed true}
   [:frames :some] ; FIXME actually images
   [:frame-duration pos?]
   [:looping? :boolean]])
