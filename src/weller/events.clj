;  Weller is like Alfresco out-of-process extensions but 100% Clojure
;  Copyright (C) 2024 Saidone
;
;  This program is free software: you can redistribute it and/or modify
;  it under the terms of the GNU General Public License as published by
;  the Free Software Foundation, either version 3 of the License, or
;  (at your option) any later version.
;
;  This program is distributed in the hope that it will be useful,
;  but WITHOUT ANY WARRANTY; without even the implied warranty of
;  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;  GNU General Public License for more details.
;
;  You should have received a copy of the GNU General Public License
;  along with this program.  If not, see <http://www.gnu.org/licenses/>.

(ns weller.events)

(def ^:const node-created "org.alfresco.event.node.Created")
(def ^:const node-updated "org.alfresco.event.node.Updated")
(def ^:const node-deleted "org.alfresco.event.node.Deleted")
(def ^:const child-assoc-created "org.alfresco.event.assoc.child.Created")
(def ^:const child-assoc-deleted "org.alfresco.event.assoc.child.Deleted")
(def ^:const peer-assoc-created "org.alfresco.event.assoc.peer.Created")
(def ^:const peer-assoc-deleted "org.alfresco.event.assoc.peer.Deleted")
(def ^:const permission-updated "org.alfresco.event.permission.Updated")