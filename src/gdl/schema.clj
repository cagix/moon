(ns gdl.schema
  (:refer-clojure :exclude [type]))

(defn type [schema]
  (if (vector? schema)
    (schema 0)
    schema))

; why do we need to know about malli/malli-forms -> just make API for schema ...
; its just validating/optional-keys ....
(defmulti malli-form type)
(defmethod malli-form :default [schema] schema)

; why is the schema connected with property ? property is just a key

; and (s/def ?)

; I want an totally abstract 'schemas' and know what is my API ....

; => this is also a good idea shortes names possible
; 'schema-type' => different namespace
; => look at your code from this angle...

; Wait!
; => schema/of gets schema of a _KEY_
; :properties/creatures is the key
; so the database consists of properties
; map of
{:properties/creatures
 :properties/audiovisuals
 ; etc. ...
 ; so the whole DB as one thing will be validated.
 }

; so what is then the 'type' of any one property?
; ....

