# Weller
![](https://upload.wikimedia.org/wikipedia/commons/d/de/Sam-weller-kyd.jpeg)

Weller is like Alfresco out-of-process extensions but in [Clojure](https://clojure.org).

[![Clojars Version](https://img.shields.io/clojars/v/org.saidone/weller)](https://clojars.org/org.saidone/weller)
[![cljdoc badge](https://cljdoc.org/badge/org.saidone/weller)](https://cljdoc.org/d/org.saidone/weller)

## Outline
- Lean and readable code
- 100% pure Clojure
- 100% test coverage
- Benefits from the [REPL](https://clojure.org/guides/repl/introduction) programming

## Usage
### Create filters by composing predicates
Filters are used for selecting messages that will flow through a tap. Filters can discriminate a message by matching
several conditions (e.g. a node that has been created or deleted, an aspect that has been added to a node, a property
that changed its value, etc.).

Filters can be made of a single predicate:
```clojure
(pred/event? events/node-updated)
```
or by a composition of predicates using `every-pred` (logical AND) or `some-fn` (logical OR) Clojure built-in functions.

E.g. this matches every updated node which is also file:
```clojure
(every-pred (pred/event? events/node-updated)
            (pred/is-file?))
```
this one matches every node with `cm:titled` **or** `cm:dublincore` aspects (regardless of the event type): 
```clojure
(some-fn (pred/node-aspect? cm/asp-titled)
         (pred/node-aspect? cm/asp-dublincore))
```
and this one matches updated nodes with `cm:titled` **or** `cm:dublincore` aspects:
```clojure
(every-pred (pred/event? events/node-updated)
            (some-fn (pred/node-aspect? cm/asp-titled)
                     (pred/node-aspect? cm/asp-dublincore)))
```
The built-in predicates are available [here](src/weller/predicates.clj) while the events [here](src/weller/events.clj).
### Create a function
A (processing) function is the piece of code deputed to take the (resource part of) message and do something with it.
The (node) resource is a map representing (usually) a node in Alfresco.

A simple function that prints the node name could be:
```clojure
(defn print-node-name
  [resource]
  (println (:name resource)))
```
a more useful function that make use of the [CRAL](https://github.com/saidone75/cral) library to update the node on
Alfresco:
```clojure
(defn add-aspect
  [resource]
  (let [aspect-names (get-in (nodes/get-node (:ticket @c/config) (:id resource)) [:body :entry :aspect-names])]
    (->> (model/map->UpdateNodeBody {:aspect-names (conj aspect-names cm/asp-dublincore)})
         (nodes/update-node (:ticket @c/config) (:id resource)))))
```
Note that the (resource part of) message is automatically converted to a plain Clojure map with keys changed to
kebab-case and keywordized thus looks like this (representing a node in this case):
```
{:primary-assoc-qname "cm:49d3a98c-6a2d-4851-a3a5-6de719033b90",
 :properties
 {:cm:auto-version true,
  :cm:version-type "MAJOR",
  :cm:auto-version-on-update-props false,
  :cm:version-label "1.0",
  :cm:initial-version true},
 :secondary-parents [],
 :is-file true,
 :is-folder false,
 :created-by-user {:id "admin", :display-name "Administrator"},
 :content
 {:mime-type "application/octet-stream",
  :size-in-bytes 0,
  :encoding "UTF-8"},
 :primary-hierarchy
 ["71290ab7-c07d-424e-a8b4-2eb3aefdd434"
  "7cf72ac7-addc-4fe5-8af2-26d50dc9d575"
  "81482fa8-3079-4187-96ae-43e36353d2c1"],
 :name "49d3a98c-6a2d-4851-a3a5-6de719033b90",
 :modified-at "2024-05-17T12:25:06.629Z",
 :node-type "cm:content",
 :id "8b799b25-6e1e-4ec9-92ad-7db2d6e5598e",
 :modified-by-user {:id "admin", :display-name "Administrator"},
 :aspect-names ["cm:versionable" "cm:auditable"],
 :@type "NodeResource",
 :created-at "2024-05-17T12:25:06.567Z"}
```
### Configuration
Configuration is stored internally in the atom `weller.config/config`. It contains a map like this:
```clojure
{:alfresco {:scheme         "http"
            :host           "localhost"
            :port           8080
            :core-path      "alfresco/api/-default-/public/alfresco/versions/1"
            :search-path    "alfresco/api/-default-/public/search/versions/1"
            :auth-path      "alfresco/api/-default-/public/authentication/versions/1"
            :discovery-path "alfresco/api/discovery"
            :user           "admin"
            :password       "admin"}
 :activemq {:scheme "tcp"
            :host   "localhost"
            :port   61616
            :topic  "alfresco.repo.event2"}}
```
When a message pipe is started then `weller.config/configure` is called and will try to load these files (in order):
```clojure
(def ^:private cfg-files
  ["resources/weller.edn"
   "~/.weller/weller.edn"
   "./weller.edn"])
```
each (map contained in) file is (deep) merged with the previous, and values being overwritten by the most recent.
Placeholders for environment variables can be used, e.g.:
```clojure
{:alfresco {:user     "${ALF_USER:admin}"
            :password "${ALF_PASS:admin}"}}
```
will look for ALF_USER and ALF_PASS environment variables if defined, otherwise the default value will be used.
Environment variables will win over the configuration files.

Lastly `cral.config/configure` is called and an Alfresco ticket is requested and then stored in `:alfresco :ticket`
section of the map. It can be retrieved with `(get-in @c/config [:alfresco :ticket])` (assuming that config has been
imported as `[weller.config :as c]`).
### Build and start a message pipe
The standard constructor `make-pipe` will create a pipe that receives ActiveMQ messages. Then at least one tap must be
connected using the function `add-filtered-tap` that takes a filter and a (processing) function as arguments (note that
in Clojure functions are first-class and can be passed-to or returned-from other functions). Finally the pipe can be
started manually by calling `component/start` on it.
```clojure
(-> (pipe/make-pipe)
    (pipe/add-filtered-tap (pred/event? events/node-created) process-created-node)
    (pipe/add-filtered-tap (pred/event? events/node-deleted) process-deleted-node)
    (component/start))
```
The *quick* constructor takes directly a filter and a function. A tap will be created and connected internally and the
pipe started automatically:
```clojure
(pipe/make-pipe (pred/event? events/node-created) process-created-node)
```
## License
Copyright (c) 2024 Saidone

Distributed under the GNU General Public License v3.0
