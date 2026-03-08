export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: string;
}

export interface UserProfile {
  id: number;
  username: string;
  displayName: string;
  role: string;
}

export interface AuthPayload {
  token: string;
  user: UserProfile;
}
