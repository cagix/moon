; Release as separate library with javafx app
; and way to add schemas inside the app (build up your database....)
; and decouple schema from malli etc.
; even how the property-types are handled.....?

; wait why do I even need property-types ?
; we just validate each property against its key
; as we know we are dealing with a thing when we use it ?

; even the dev-loop itself....


; Idea: property/type remove, make whole 'db-schema' instead of schemas
; db is map of {:audiovisuals, creatures, etc.}
; and we know when we fetch something against what to check it ?
; also complete db schema also textures -> image :file is getting to texture
; file is one-to-one texture ? or spritesheets we can have as separate properties ?!

; But first make context-free db so I can abstract mallis/schema, .... its just API -> optional-map-keys/validate something ...

; How does datomic do it ?
; why don't I use datomic?

; also for worlds  implement [:s/or ...] then can just simply add 'or-widget'


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

